package com.kdongd.nrnl.service;

import com.kdongd.nrnl.domain.TechniqueType;
import com.kdongd.nrnl.domain.BoxingWorkout;
import com.kdongd.nrnl.domain.RunningWorkout;
import com.kdongd.nrnl.domain.User;
import com.kdongd.nrnl.domain.Workout;
import com.kdongd.nrnl.domain.WorkoutType;
import com.kdongd.nrnl.dto.WorkoutDetailForm;
import com.kdongd.nrnl.dto.WorkoutForm;
import com.kdongd.nrnl.dto.WorkoutMonthlyStat;
import com.kdongd.nrnl.dto.WorkoutStatByType;
import com.kdongd.nrnl.exception.InvalidSortPropertyException;
import com.kdongd.nrnl.exception.WorkoutNotFoundException;
import com.kdongd.nrnl.exception.WorkoutTypeMismatchException;
import com.kdongd.nrnl.repository.UserRepository;
import com.kdongd.nrnl.repository.WorkoutQueryRepository;
import com.kdongd.nrnl.repository.WorkoutRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock
    private WorkoutRepository repository;

    @Mock
    private WorkoutQueryRepository queryRepository;

    @Mock
    private UserRepository userRepository;

    private final LocalDateTime now = LocalDateTime.now();

    private final User owner = createOwnerWithId(1L);

    private static User createOwnerWithId(Long id) {
        User user = User.create("test@test.com", "encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private WorkoutService service() {
        return new WorkoutService(repository, queryRepository, userRepository);
    }

    @Test
    @DisplayName("운동 기록 저장 후 반환")
    void save() {
        WorkoutForm form = new WorkoutForm(
                WorkoutType.RUNNING, 30, "테스트 메모", now, null,
                5.0, "한강", 300,
                null, null, null);
        RunningWorkout saved = RunningWorkout.create(owner, 30, "테스트 메모", now, 5.0, "한강", 300);
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(repository.save(any())).willReturn(saved);

        Workout result = service().save(form, 1L);

        assertThat(result.getType()).isEqualTo(WorkoutType.RUNNING);
        assertThat(result.getDurationMinutes()).isEqualTo(30);
        assertThat(result.getMemo()).isEqualTo("테스트 메모");
    }

    @Test
    @DisplayName("details 포함 운동 기록 저장")
    void save_withDetails() {
        List<WorkoutDetailForm> details = List.of(
                new WorkoutDetailForm(1, "1라운드", 180, "섀도우")
        );
        WorkoutForm form = new WorkoutForm(
                WorkoutType.BOXING, 60, "메모", now, details,
                null, null, null,
                3, "파트너", TechniqueType.SPARRING);
        BoxingWorkout saved = BoxingWorkout.create(owner, 60, "메모", now, 3, "파트너", TechniqueType.SPARRING);
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(repository.save(any())).willReturn(saved);

        Workout result = service().save(form, 1L);

        assertThat(result.getType()).isEqualTo(WorkoutType.BOXING);
    }

    @Test
    @DisplayName("존재하는 id 조회 성공")
    void findById_success() {
        RunningWorkout workout = RunningWorkout.create(owner, 30, "메모", now, 5.0, "한강", 300);
        given(repository.findById(1L)).willReturn(Optional.of(workout));

        Workout result = service().findById(1L, 1L);

        assertThat(result.getType()).isEqualTo(WorkoutType.RUNNING);
    }

    @Test
    @DisplayName("존재하지 않는 id 조회 시 WorkoutNotFoundException 발생")
    void findById_notFound() {
        given(repository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(999L, 1L))
                .isInstanceOf(WorkoutNotFoundException.class);
    }

    @Test
    @DisplayName("다른 유저 소유의 운동 기록을 조회하려 하면 WorkoutNotFoundException이 발생한다 (존재는 하지만 접근 권한이 없다는 사실 자체를 노출하지 않음)")
    void findById_otherUsersWorkout_throwsWorkoutNotFoundException() {
        RunningWorkout workout = RunningWorkout.create(owner, 30, "메모", now, 5.0, "한강", 300);
        given(repository.findById(1L)).willReturn(Optional.of(workout));

        assertThatThrownBy(() -> service().findById(1L, 2L))
                .isInstanceOf(WorkoutNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 id 삭제 시 WorkoutNotFoundException 발생")
    void delete_notFound() {
        given(repository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(999L, 1L))
                .isInstanceOf(WorkoutNotFoundException.class);
    }

    @Test
    @DisplayName("존재하는 id 삭제 성공 시 repository.delete()가 실제로 호출된다")
    void delete_success() {
        RunningWorkout workout = RunningWorkout.create(owner, 30, "메모", now, 5.0, "한강", 300);
        given(repository.findById(1L)).willReturn(Optional.of(workout));

        service().delete(1L, 1L);

        verify(repository).delete(workout);
    }

    @Test
    @DisplayName("다른 유저 소유의 운동 기록을 삭제하려 하면 WorkoutNotFoundException이 발생하고 실제 삭제는 호출되지 않는다")
    void delete_otherUsersWorkout_throwsWorkoutNotFoundExceptionAndDoesNotDelete() {
        RunningWorkout workout = RunningWorkout.create(owner, 30, "메모", now, 5.0, "한강", 300);
        given(repository.findById(1L)).willReturn(Optional.of(workout));

        assertThatThrownBy(() -> service().delete(1L, 2L))
                .isInstanceOf(WorkoutNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 id 수정 시 WorkoutNotFoundException 발생")
    void update_notFound() {
        given(repository.findById(999L)).willReturn(Optional.empty());
        WorkoutForm form = new WorkoutForm(
                WorkoutType.RUNNING, 30, "메모", now, null,
                5.0, "한강", 300,
                null, null, null);

        assertThatThrownBy(() -> service().update(999L, form, 1L))
                .isInstanceOf(WorkoutNotFoundException.class);
    }

    @Test
    @DisplayName("다른 유저 소유의 운동 기록을 수정하려 하면 WorkoutNotFoundException이 발생한다")
    void update_otherUsersWorkout_throwsWorkoutNotFoundException() {
        RunningWorkout existing = RunningWorkout.create(owner, 30, "메모", now, 5.0, "한강", 300);
        given(repository.findById(1L)).willReturn(Optional.of(existing));
        WorkoutForm form = new WorkoutForm(
                WorkoutType.RUNNING, 30, "메모", now, null,
                5.0, "한강", 300,
                null, null, null);

        assertThatThrownBy(() -> service().update(1L, form, 2L))
                .isInstanceOf(WorkoutNotFoundException.class);
    }

    @Test
    @DisplayName("기존 타입과 다른 타입으로 수정 요청 시 WorkoutTypeMismatchException 발생")
    void update_typeMismatch_throwsException() {
        RunningWorkout existing = RunningWorkout.create(owner, 30, "메모", now, 5.0, "한강", 300);
        given(repository.findById(1L)).willReturn(Optional.of(existing));

        WorkoutForm form = new WorkoutForm(
                WorkoutType.BOXING, 60, "메모", now, null,
                null, null, null,
                3, "파트너", TechniqueType.SPARRING);

        assertThatThrownBy(() -> service().update(1L, form, 1L))
                .isInstanceOf(WorkoutTypeMismatchException.class);
    }

    @Test
    @DisplayName("같은 타입으로 수정하면 필드가 갱신되고 저장된 엔티티를 반환한다")
    void update_success() {
        RunningWorkout existing = RunningWorkout.create(owner, 30, "이전 메모", now, 5.0, "한강", 300);
        given(repository.findById(1L)).willReturn(Optional.of(existing));

        WorkoutForm form = new WorkoutForm(
                WorkoutType.RUNNING, 60, "수정된 메모", now, null,
                10.0, "남산", 500,
                null, null, null);

        Workout result = service().update(1L, form, 1L);

        assertThat(result.getDurationMinutes()).isEqualTo(60);
        assertThat(result.getMemo()).isEqualTo("수정된 메모");
        assertThat(((RunningWorkout) result).getDistanceKm()).isEqualTo(10.0);
        assertThat(((RunningWorkout) result).getPlace()).isEqualTo("남산");
    }

    @Test
    @DisplayName("수정 시 details가 통째로 교체된다")
    void update_replacesDetails() {
        BoxingWorkout existing = BoxingWorkout.create(owner, 60, "메모", now, 3, "파트너", TechniqueType.SPARRING);
        WorkoutDetailForm oldDetailForm = new WorkoutDetailForm(1, "기존 라운드", 180, null);
        existing.addDetail(
                com.kdongd.nrnl.domain.WorkoutDetail.create(
                        existing, oldDetailForm.sequence(), oldDetailForm.label(),
                        oldDetailForm.durationSeconds(), oldDetailForm.note()));
        given(repository.findById(1L)).willReturn(Optional.of(existing));

        List<WorkoutDetailForm> newDetails = List.of(
                new WorkoutDetailForm(1, "새 라운드", 200, "미트")
        );
        WorkoutForm form = new WorkoutForm(
                WorkoutType.BOXING, 60, "메모", now, newDetails,
                null, null, null,
                3, "파트너", TechniqueType.SPARRING);

        Workout result = service().update(1L, form, 1L);

        assertThat(result.getDetails()).hasSize(1);
        assertThat(result.getDetails().get(0).getLabel()).isEqualTo("새 라운드");
    }

    @Test
    @DisplayName("허용된 정렬 필드로 검색하면 queryRepository.search()에 위임한다")
    void search_validSort_delegatesToQueryRepository() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("workoutDateTime"));
        RunningWorkout workout = RunningWorkout.create(owner, 30, "메모", now, 5.0, "한강", 300);
        Page<Workout> page = new PageImpl<>(List.of(workout));
        given(userRepository.existsById(1L)).willReturn(true);
        given(queryRepository.search(1L, WorkoutType.RUNNING, null, null, pageable)).willReturn(page);

        Page<Workout> result = service().search(WorkoutType.RUNNING, null, null, pageable, 1L);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(WorkoutType.RUNNING);
    }

    @Test
    @DisplayName("허용되지 않은 정렬 필드로 검색하면 InvalidSortPropertyException이 발생한다")
    void search_invalidSort_throwsException() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("memo"));

        assertThatThrownBy(() -> service().search(null, null, null, pageable, 1L))
                .isInstanceOf(InvalidSortPropertyException.class);
    }

    @Test
    @DisplayName("정렬 필드가 화이트리스트에 없으면 queryRepository.search()가 호출되지 않는다")
    void search_invalidSort_doesNotCallRepository() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));

        assertThatThrownBy(() -> service().search(null, null, null, pageable, 1L))
                .isInstanceOf(InvalidSortPropertyException.class);

        verify(queryRepository, never()).search(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("타입별 통계는 queryRepository에 위임한다")
    void statsByType_delegatesToQueryRepository() {
        List<WorkoutStatByType> stats = List.of(new WorkoutStatByType(WorkoutType.RUNNING, 3L, 90L));
        given(userRepository.existsById(1L)).willReturn(true);
        given(queryRepository.statsByType(1L, null, null)).willReturn(stats);

        List<WorkoutStatByType> result = service().statsByType(null, null, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(WorkoutType.RUNNING);
    }

    @Test
    @DisplayName("월별 통계는 queryRepository에 위임한다")
    void statsByMonth_delegatesToQueryRepository() {
        List<WorkoutMonthlyStat> stats = List.of(new WorkoutMonthlyStat(2026, 5, 4L));
        given(userRepository.existsById(1L)).willReturn(true);
        given(queryRepository.statsByMonth(1L, null, null)).willReturn(stats);

        List<WorkoutMonthlyStat> result = service().statsByMonth(null, null, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).year()).isEqualTo(2026);
    }

    @Test
    @DisplayName("기간 필터를 지정하면 queryRepository에 from/to가 그대로 전달된다")
    void statsByType_withDateRange_passesThroughToQueryRepository() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 31, 23, 59);
        List<WorkoutStatByType> stats = List.of(new WorkoutStatByType(WorkoutType.RUNNING, 1L, 30L));
        given(userRepository.existsById(1L)).willReturn(true);
        given(queryRepository.statsByType(1L, from, to)).willReturn(stats);

        List<WorkoutStatByType> result = service().statsByType(from, to, 1L);

        assertThat(result).hasSize(1);
        verify(queryRepository).statsByType(1L, from, to);
    }
}
