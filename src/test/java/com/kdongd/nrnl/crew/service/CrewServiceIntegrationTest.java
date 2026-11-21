package com.kdongd.nrnl.crew.service;

import com.kdongd.nrnl.crew.domain.CrewApplicationStatus;
import com.kdongd.nrnl.crew.domain.CrewStatus;
import com.kdongd.nrnl.crew.dto.CrewApplicationResponse;
import com.kdongd.nrnl.crew.dto.CrewCreateRequest;
import com.kdongd.nrnl.crew.dto.CrewResponse;
import com.kdongd.nrnl.crew.dto.CrewUpdateRequest;
import com.kdongd.nrnl.crew.exception.CrewException;
import com.kdongd.nrnl.domain.User;
import com.kdongd.nrnl.domain.WorkoutType;
import com.kdongd.nrnl.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class CrewServiceIntegrationTest {

    private static final String DESCRIPTION = "러닝과 복싱을 함께 하는 크루입니다";
    private static final Set<WorkoutType> SPORTS = Set.of(WorkoutType.RUNNING, WorkoutType.BOXING);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private CrewService service;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void publicSearchFiltersCountsAndPagesInLatestOrder() {
        String prefix = "한글" + suffix();
        long firstLeader = user();
        CrewResponse first = create(prefix + "Run", firstLeader);
        CrewResponse second = service.create(new CrewCreateRequest(prefix + "Box", DESCRIPTION,
                Set.of(WorkoutType.BOXING), 30), user());
        long applicant = user();
        service.approve(first.id(), service.apply(first.id(), applicant).id(), firstLeader);

        assertThat(first.status()).isEqualTo(CrewStatus.OPEN);
        assertThat(first.sports()).containsExactlyInAnyOrderElementsOf(SPORTS);
        assertThat(service.findById(first.id()).memberCount()).isEqualTo(2);
        assertThat(service.search(null, null, null, PageRequest.of(0, 10)).getContent()).isNotEmpty();
        var firstPage = service.search(prefix, WorkoutType.BOXING, CrewStatus.OPEN,
                PageRequest.of(0, 1, Sort.by("name").ascending()));
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(CrewResponse::id).containsExactly(second.id());
        assertThat(service.search(prefix, WorkoutType.BOXING, CrewStatus.OPEN, PageRequest.of(1, 1))
                .getContent()).extracting(CrewResponse::id).containsExactly(first.id());
        assertThat(service.search(prefix + "rUN", WorkoutType.RUNNING, CrewStatus.OPEN, PageRequest.of(0, 10))
                .getContent()).singleElement().satisfies(crew -> assertThat(crew.memberCount()).isEqualTo(2));
        assertThat(service.search("%", null, CrewStatus.OPEN, PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void retainsApplicationHistoryAndAllowsReapplyAfterCancellationRejectionAndExpulsion() {
        long leader = user();
        long applicant = user();
        long outsider = user();
        CrewResponse crew = create("Flow" + suffix(), leader);
        CrewApplicationResponse first = service.apply(crew.id(), applicant);

        assertConflict(() -> service.apply(crew.id(), applicant));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.cancel(crew.id(), first.id(), outsider));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.approve(crew.id(), first.id(), outsider));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.findApplications(crew.id(), outsider, PageRequest.of(0, 10)));
        assertThat(service.cancel(crew.id(), first.id(), applicant).status()).isEqualTo(CrewApplicationStatus.CANCELLED);

        CrewApplicationResponse second = service.apply(crew.id(), applicant);
        assertThat(service.reject(crew.id(), second.id(), leader).status()).isEqualTo(CrewApplicationStatus.REJECTED);
        CrewApplicationResponse third = service.apply(crew.id(), applicant);
        assertStatus(HttpStatus.FORBIDDEN, () -> service.update(crew.id(), update(crew, 30, CrewStatus.CLOSED), outsider));
        service.update(crew.id(), update(crew, 30, CrewStatus.CLOSED), leader);

        assertConflict(() -> service.apply(crew.id(), outsider));
        assertThat(service.approve(crew.id(), third.id(), leader).status()).isEqualTo(CrewApplicationStatus.APPROVED);
        assertConflict(() -> service.cancel(crew.id(), third.id(), applicant));
        assertThat(service.findById(crew.id()).memberCount()).isEqualTo(2);
        assertStatus(HttpStatus.FORBIDDEN, () -> service.expel(crew.id(), outsider, applicant));
        service.expel(crew.id(), leader, applicant);
        assertConflict(() -> service.update(crew.id(), update(crew, 10, CrewStatus.OPEN), leader));
        service.update(crew.id(), update(crew, 30, CrewStatus.OPEN), leader);
        assertThat(service.apply(crew.id(), applicant).status()).isEqualTo(CrewApplicationStatus.PENDING);
        assertThat(service.findApplications(crew.id(), leader, PageRequest.of(0, 10)))
                .extracting(CrewApplicationResponse::status)
                .containsExactly(CrewApplicationStatus.PENDING, CrewApplicationStatus.APPROVED,
                        CrewApplicationStatus.REJECTED, CrewApplicationStatus.CANCELLED);

        assertStatus(HttpStatus.FORBIDDEN, () -> service.delete(crew.id(), applicant));
        service.delete(crew.id(), leader);
        for (String table : List.of("crew_sport", "crew_member", "crew_application")) {
            assertThat(jdbc.queryForObject("select count(*) from " + table + " where crew_id = ?",
                    Long.class, crew.id())).isZero();
        }
        assertThat(create(crew.name().toLowerCase(java.util.Locale.ROOT), leader).memberCount()).isEqualTo(1);
    }

    @Test
    void transfersLeadershipOnlyToAnEligibleMemberAndKeepsFormerLeaderUntilTheyLeave() {
        long leader = user();
        long target = user();
        long outsider = user();
        CrewResponse crew = create("Lead" + suffix(), leader);
        CrewResponse otherCrew = create("Other" + suffix(), target);
        service.approve(crew.id(), service.apply(crew.id(), target).id(), leader);

        assertConflict(() -> service.leave(crew.id(), leader));
        assertConflict(() -> service.expel(crew.id(), leader, leader));
        assertConflict(() -> create("Second" + suffix(), leader));
        assertConflict(() -> service.transferLeadership(crew.id(), leader, target));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.transferLeadership(crew.id(), leader, outsider));

        service.delete(otherCrew.id(), target);
        service.transferLeadership(crew.id(), leader, target);
        assertThat(service.findMembers(crew.id())).hasSize(2)
                .anySatisfy(member -> {
                    assertThat(member.userId()).isEqualTo(leader);
                    assertThat(member.leader()).isFalse();
                })
                .anySatisfy(member -> {
                    assertThat(member.userId()).isEqualTo(target);
                    assertThat(member.leader()).isTrue();
                });
        assertStatus(HttpStatus.FORBIDDEN, () -> service.update(crew.id(), update(crew, 30, CrewStatus.OPEN), leader));
        service.leave(crew.id(), leader);
        assertThat(service.findById(crew.id()).memberCount()).isEqualTo(1);
    }

    @Test
    void concurrentApprovalsCannotClaimTheSameFinalSeat() throws Exception {
        long leader = user();
        CrewResponse crew = create("Capacity" + suffix(), leader);
        for (int i = 0; i < 8; i++) {
            service.approve(crew.id(), service.apply(crew.id(), user()).id(), leader);
        }
        CrewApplicationResponse first = service.apply(crew.id(), user());
        CrewApplicationResponse second = service.apply(crew.id(), user());

        assertThat(race(() -> service.approve(crew.id(), first.id(), leader),
                () -> service.approve(crew.id(), second.id(), leader))).containsExactlyInAnyOrder(true, false);
        assertThat(service.findById(crew.id()).memberCount()).isEqualTo(10);
        long lateApplicant = user();
        assertConflict(() -> service.apply(crew.id(), lateApplicant));
        assertThat(service.findApplications(crew.id(), leader, PageRequest.of(0, 20)))
                .filteredOn(application -> application.status() == CrewApplicationStatus.PENDING).hasSize(1);
    }

    @Test
    void concurrentApprovalsAcrossCrewsCannotExceedMembershipLimit() throws Exception {
        long applicant = user();
        create("Own" + suffix(), applicant);
        long secondLeader = user();
        CrewResponse secondCrew = create("Second" + suffix(), secondLeader);
        service.approve(secondCrew.id(), service.apply(secondCrew.id(), applicant).id(), secondLeader);
        long thirdLeader = user();
        long fourthLeader = user();
        CrewResponse thirdCrew = create("Third" + suffix(), thirdLeader);
        CrewResponse fourthCrew = create("Fourth" + suffix(), fourthLeader);
        CrewApplicationResponse first = service.apply(thirdCrew.id(), applicant);
        CrewApplicationResponse second = service.apply(fourthCrew.id(), applicant);

        assertThat(race(() -> service.approve(thirdCrew.id(), first.id(), thirdLeader),
                () -> service.approve(fourthCrew.id(), second.id(), fourthLeader))).containsExactlyInAnyOrder(true, false);
        assertThat(jdbc.queryForObject("select count(*) from crew_member where user_id = ?",
                Long.class, applicant)).isEqualTo(3);
        CrewResponse fifthCrew = create("Fifth" + suffix(), user());
        assertThat(service.apply(fifthCrew.id(), applicant).status()).isEqualTo(CrewApplicationStatus.PENDING);
    }

    @Test
    void concurrentTransfersAcrossCrewsCannotGiveOneMemberTwoLeaderships() throws Exception {
        long target = user();
        long firstLeader = user();
        long secondLeader = user();
        CrewResponse first = create("TransferA" + suffix(), firstLeader);
        CrewResponse second = create("TransferB" + suffix(), secondLeader);
        service.approve(first.id(), service.apply(first.id(), target).id(), firstLeader);
        service.approve(second.id(), service.apply(second.id(), target).id(), secondLeader);

        assertThat(race(() -> service.transferLeadership(first.id(), firstLeader, target),
                () -> service.transferLeadership(second.id(), secondLeader, target))).containsExactlyInAnyOrder(true, false);
        assertThat(jdbc.queryForObject("select count(*) from crew where leader_id = ?", Long.class, target)).isEqualTo(1);
    }

    @Test
    void databaseEnforcesNamePendingMembershipAndLeaderConstraints() throws Exception {
        long leader = user();
        long applicant = user();
        long other = user();
        String suffix = suffix();
        CrewResponse crew = create("Duplicate" + suffix, leader);
        assertConflict(() -> create("dUPLICATE" + suffix, other));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.create(
                new CrewCreateRequest("Invalid" + suffix, DESCRIPTION, SPORTS, 20), other));

        long competingLeader = user();
        assertThat(race(() -> create("Race" + suffix, other), () -> create("rACE" + suffix, competingLeader)))
                .containsExactlyInAnyOrder(true, false);
        service.apply(crew.id(), applicant);

        assertThatThrownBy(() -> jdbc.update("update crew set capacity = 20 where id = ?", crew.id()))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("ck_crew_capacity");
        assertThatThrownBy(() -> jdbc.update("update crew set name = 'bad name' where id = ?", crew.id()))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("ck_crew_name");
        assertThatThrownBy(() -> jdbc.update("insert into crew_member(crew_id, user_id) values (?, ?)", crew.id(), leader))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("uk_crew_member");
        assertThatThrownBy(() -> jdbc.update("""
                insert into crew_application(crew_id, user_id, status, created_at)
                values (?, ?, 'PENDING', now())
                """, crew.id(), applicant))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("uk_crew_application_pending");
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                jdbc.update("delete from crew_member where crew_id = ? and user_id = ?", crew.id(), leader)))
                .hasStackTraceContaining("fk_crew_leader_membership");
    }

    private CrewResponse create(String name, long leader) {
        return service.create(new CrewCreateRequest(name, DESCRIPTION, SPORTS, 10), leader);
    }

    private CrewUpdateRequest update(CrewResponse crew, int capacity, CrewStatus status) {
        return new CrewUpdateRequest(crew.name(), DESCRIPTION, SPORTS, capacity, status);
    }

    private long user() {
        return users.save(User.create(UUID.randomUUID() + "@test.com", "encoded-password")).getId();
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void assertConflict(Runnable operation) {
        assertStatus(HttpStatus.CONFLICT, operation);
    }

    private void assertStatus(HttpStatus status, Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(CrewException.class,
                error -> assertThat(error.getStatus()).isEqualTo(status));
    }

    private List<Boolean> race(Runnable first, Runnable second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> left = executor.submit(racer(first, ready, start));
            Future<Boolean> right = executor.submit(racer(second, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(left.get(15, TimeUnit.SECONDS), right.get(15, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> racer(Runnable operation, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 요청 시작 대기 시간 초과");
            }
            try {
                operation.run();
                return true;
            } catch (CrewException e) {
                assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                return false;
            }
        };
    }
}
