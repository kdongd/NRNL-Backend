package com.kdongd.nrnl.crew.service;

import com.kdongd.nrnl.crew.domain.Crew;
import com.kdongd.nrnl.crew.domain.CrewApplication;
import com.kdongd.nrnl.crew.domain.CrewApplicationStatus;
import com.kdongd.nrnl.crew.domain.CrewMember;
import com.kdongd.nrnl.crew.domain.CrewStatus;
import com.kdongd.nrnl.crew.dto.CrewApplicationResponse;
import com.kdongd.nrnl.crew.dto.CrewCreateRequest;
import com.kdongd.nrnl.crew.dto.CrewMemberResponse;
import com.kdongd.nrnl.crew.dto.CrewResponse;
import com.kdongd.nrnl.crew.dto.CrewUpdateRequest;
import com.kdongd.nrnl.crew.exception.CrewException;
import com.kdongd.nrnl.crew.repository.CrewApplicationRepository;
import com.kdongd.nrnl.crew.repository.CrewMemberRepository;
import com.kdongd.nrnl.crew.repository.CrewRepository;
import com.kdongd.nrnl.crew.repository.CrewUserRepository;
import com.kdongd.nrnl.domain.User;
import com.kdongd.nrnl.domain.WorkoutType;
import com.kdongd.nrnl.exception.AuthenticatedUserNotFoundException;
import com.kdongd.nrnl.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CrewService {

    private static final int MAX_MEMBERSHIPS = 3;
    private static final Set<Integer> CAPACITIES = Set.of(10, 30, 50, 70, 100);

    private final CrewRepository crews;
    private final CrewMemberRepository members;
    private final CrewApplicationRepository applications;
    private final CrewUserRepository crewUsers;
    private final UserRepository users;
    private final Clock clock;

    public CrewService(CrewRepository crews, CrewMemberRepository members,
                       CrewApplicationRepository applications, CrewUserRepository crewUsers,
                       UserRepository users, Clock clock) {
        this.crews = crews;
        this.members = members;
        this.applications = applications;
        this.crewUsers = crewUsers;
        this.users = users;
        this.clock = clock;
    }

    public CrewResponse create(CrewCreateRequest request, Long userId) {
        validateCapacity(request.capacity());
        User leader = lockUser(userId);
        requireNoLeadership(userId);
        requireMembershipSlot(userId);

        Crew crew = Crew.create(userId, request.name(), request.description(),
                request.sports(), request.capacity(), clock.instant());
        saveCrew(crew);
        members.save(CrewMember.create(crew.getId(), leader));
        return CrewResponse.from(crew, 1);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<CrewResponse> search(String name, WorkoutType sport, CrewStatus status, Pageable pageable) {
        return crews.search(name == null ? "" : name.toLowerCase(Locale.ROOT), sport == null ? WorkoutType.RUNNING : sport,
                        sport == null, status, unsorted(pageable))
                .map(row -> CrewResponse.from(row.getCrew(), row.getMemberCount()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CrewResponse findById(Long crewId) {
        return CrewResponse.from(findCrew(crewId), members.countByCrewId(crewId));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<CrewMemberResponse> findMembers(Long crewId) {
        Crew crew = findCrew(crewId);
        return members.findAllByCrewIdOrderById(crewId).stream()
                .map(CrewMember::getUser)
                .map(user -> new CrewMemberResponse(user.getId(), user.getNickname(),
                        crew.getLeaderId().equals(user.getId())))
                .toList();
    }

    public CrewResponse update(Long crewId, CrewUpdateRequest request, Long userId) {
        Crew crew = lockCrew(crewId);
        requireLeader(crew, userId);
        validateCapacity(request.capacity());
        if (request.capacity() < crew.getCapacity()) {
            throw CrewException.conflict("크루 정원은 줄일 수 없습니다");
        }
        crew.update(request.name(), request.description(), request.sports(), request.capacity(), request.status());
        saveCrew(crew);
        return CrewResponse.from(crew, members.countByCrewId(crewId));
    }

    public void delete(Long crewId, Long userId) {
        Crew crew = lockCrew(crewId);
        requireLeader(crew, userId);
        crews.delete(crew);
    }

    public CrewApplicationResponse apply(Long crewId, Long userId) {
        Crew crew = lockCrew(crewId);
        if (crew.getStatus() == CrewStatus.CLOSED) {
            throw CrewException.conflict("모집이 마감되었습니다");
        }
        requireCapacity(crew);
        requireNonMember(crewId, userId);
        if (applications.existsByCrewIdAndUserIdAndStatus(crewId, userId, CrewApplicationStatus.PENDING)) {
            throw CrewException.conflict("이미 대기 중인 가입 신청이 있습니다");
        }
        User applicant = users.findById(userId).orElseThrow(() -> new AuthenticatedUserNotFoundException(userId));
        return CrewApplicationResponse.from(applications.save(CrewApplication.create(crewId, applicant, clock.instant())));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<CrewApplicationResponse> findApplications(Long crewId, Long userId, Pageable pageable) {
        requireLeader(findCrew(crewId), userId);
        return applications.findAllByCrewIdOrderByCreatedAtDescIdDesc(crewId, unsorted(pageable))
                .map(CrewApplicationResponse::from);
    }

    public CrewApplicationResponse cancel(Long crewId, Long applicationId, Long userId) {
        lockCrew(crewId);
        CrewApplication application = findApplication(crewId, applicationId);
        if (!application.getUser().getId().equals(userId)) {
            throw CrewException.forbidden("가입 신청은 본인만 취소할 수 있습니다");
        }
        requirePending(application);
        return resolve(application, CrewApplicationStatus.CANCELLED);
    }

    public CrewApplicationResponse reject(Long crewId, Long applicationId, Long userId) {
        requireLeader(lockCrew(crewId), userId);
        CrewApplication application = findApplication(crewId, applicationId);
        requirePending(application);
        return resolve(application, CrewApplicationStatus.REJECTED);
    }

    public CrewApplicationResponse approve(Long crewId, Long applicationId, Long userId) {
        Crew crew = lockCrew(crewId);
        requireLeader(crew, userId);
        CrewApplication application = findApplication(crewId, applicationId);
        requirePending(application);
        requireCapacity(crew);

        Long applicantId = application.getUser().getId();
        User applicant = lockUser(applicantId);
        requireNonMember(crewId, applicantId);
        requireMembershipSlot(applicantId);
        members.save(CrewMember.create(crewId, applicant));
        return resolve(application, CrewApplicationStatus.APPROVED);
    }

    public void transferLeadership(Long crewId, Long userId, Long targetUserId) {
        Crew crew = lockCrew(crewId);
        requireLeader(crew, userId);
        if (!members.existsByCrewIdAndUserId(crewId, targetUserId)) {
            throw CrewException.notFound("크루원을 찾을 수 없습니다");
        }

        lockUser(targetUserId);
        requireNoLeadership(targetUserId);
        crew.transferLeadership(targetUserId);
        saveCrew(crew);
    }

    public void leave(Long crewId, Long userId) {
        Crew crew = lockCrew(crewId);
        removeMember(crew, userId);
    }

    public void expel(Long crewId, Long userId, Long targetUserId) {
        Crew crew = lockCrew(crewId);
        requireLeader(crew, userId);
        removeMember(crew, targetUserId);
    }

    private void removeMember(Crew crew, Long userId) {
        if (crew.getLeaderId().equals(userId)) {
            throw CrewException.conflict("크루장 권한을 먼저 이전해야 합니다");
        }
        CrewMember member = members.findByCrewIdAndUserId(crew.getId(), userId)
                .orElseThrow(() -> CrewException.notFound("크루원을 찾을 수 없습니다"));
        members.delete(member);
    }

    private Crew findCrew(Long id) {
        return crews.findById(id).orElseThrow(() -> CrewException.notFound("크루를 찾을 수 없습니다"));
    }

    // 기존 크루 변경은 크루 행부터 잠근다. 사용자 정책을 검사할 때만 이어서 회원 행을 잠근다.
    private Crew lockCrew(Long id) {
        return crews.findForUpdate(id).orElseThrow(() -> CrewException.notFound("크루를 찾을 수 없습니다"));
    }

    private User lockUser(Long userId) {
        return crewUsers.findForUpdate(userId).orElseThrow(() -> new AuthenticatedUserNotFoundException(userId));
    }

    private CrewApplication findApplication(Long crewId, Long applicationId) {
        return applications.findByIdAndCrewId(applicationId, crewId)
                .orElseThrow(() -> CrewException.notFound("가입 신청을 찾을 수 없습니다"));
    }

    private CrewApplicationResponse resolve(CrewApplication application, CrewApplicationStatus status) {
        application.resolve(status, clock.instant());
        return CrewApplicationResponse.from(application);
    }

    private void requireLeader(Crew crew, Long userId) {
        if (!crew.getLeaderId().equals(userId)) {
            throw CrewException.forbidden("크루장 권한이 필요합니다");
        }
    }

    private void requireNoLeadership(Long userId) {
        if (crews.existsByLeaderId(userId)) {
            throw CrewException.conflict("이미 다른 크루의 크루장입니다");
        }
    }

    private void requireMembershipSlot(Long userId) {
        if (members.countByUserId(userId) >= MAX_MEMBERSHIPS) {
            throw CrewException.conflict("크루는 최대 3개까지 소속될 수 있습니다");
        }
    }

    private void requireCapacity(Crew crew) {
        if (members.countByCrewId(crew.getId()) >= crew.getCapacity()) {
            throw CrewException.conflict("크루 정원이 가득 찼습니다");
        }
    }

    private void requireNonMember(Long crewId, Long userId) {
        if (members.existsByCrewIdAndUserId(crewId, userId)) {
            throw CrewException.conflict("이미 소속된 크루입니다");
        }
    }

    private void requirePending(CrewApplication application) {
        if (application.getStatus() != CrewApplicationStatus.PENDING) {
            throw CrewException.conflict("이미 처리된 가입 신청입니다");
        }
    }

    private void validateCapacity(Integer capacity) {
        if (capacity == null || !CAPACITIES.contains(capacity)) {
            throw CrewException.badRequest("크루 정원은 10, 30, 50, 70, 100 중 하나여야 합니다");
        }
    }

    private void saveCrew(Crew crew) {
        try {
            crews.saveAndFlush(crew);
        } catch (DataIntegrityViolationException e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "uk_crew_name".equals(violation.getConstraintName())) {
                    throw CrewException.conflict("이미 사용 중인 크루 이름입니다");
                }
            }
            throw e;
        }
    }

    private Pageable unsorted(Pageable pageable) {
        return pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                : Pageable.unpaged();
    }
}
