package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.ExperienceLevel;
import com.kdongdexample.norunnolifeexample.domain.Gender;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.domain.UserProfile;
import com.kdongdexample.norunnolifeexample.domain.WorkoutType;
import com.kdongdexample.norunnolifeexample.dto.UserProfileRequest;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.UserProfileAlreadyExistsException;
import com.kdongdexample.norunnolifeexample.repository.UserProfileRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private UserProfileService service() {
        return new UserProfileService(userRepository, userProfileRepository);
    }

    private User createUserWithId(Long id) {
        User user = User.create("user@test.com", "encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserProfileRequest sampleRequest() {
        return new UserProfileRequest(Gender.MAN, 28, 175.0, 70.0, ExperienceLevel.INTERMEDIATE,
                Set.of(WorkoutType.RUNNING), "10km 완주");
    }

    @Test
    @DisplayName("프로필이 없는 유저면 정상적으로 프로필을 생성한다")
    void createProfile_success() {
        User user = createUserWithId(1L);
        given(userProfileRepository.existsByUserId(1L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userProfileRepository.saveAndFlush(any(UserProfile.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = service().createProfile(1L, sampleRequest());

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getGender()).isEqualTo(Gender.MAN);
        assertThat(result.getAge()).isEqualTo(28);
        assertThat(result.getPrimaryExercises()).containsExactly(WorkoutType.RUNNING);
    }

    @Test
    @DisplayName("이미 프로필이 있으면 UserProfileAlreadyExistsException이 발생하고 User 조회는 호출되지 않는다")
    void createProfile_alreadyExists_throwsExceptionWithoutLookingUpUser() {
        given(userProfileRepository.existsByUserId(1L)).willReturn(true);

        assertThatThrownBy(() -> service().createProfile(1L, sampleRequest()))
                .isInstanceOf(UserProfileAlreadyExistsException.class);

        verify(userRepository, never()).findById(any());
        verify(userProfileRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("인증된 유저가 DB에 없으면 AuthenticatedUserNotFoundException이 발생한다")
    void createProfile_userNotFound_throwsException() {
        given(userProfileRepository.existsByUserId(1L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service().createProfile(1L, sampleRequest()))
                .isInstanceOf(AuthenticatedUserNotFoundException.class);

        verify(userProfileRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("existsByUserId 체크 이후 동시 요청으로 유니크 제약을 위반하면 UserProfileAlreadyExistsException으로 변환된다")
    void createProfile_concurrentDuplicate_convertsToDomainException() {
        User user = createUserWithId(1L);
        given(userProfileRepository.existsByUserId(1L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        // 실제 DB(Postgres 23505)처럼 Hibernate ConstraintViolationException을 원인으로 담아 던진다
        ConstraintViolationException uniqueViolation = new ConstraintViolationException(
                "duplicate", new SQLException("duplicate key", "23505"), "uk_user_profiles_user_id");
        willThrow(new DataIntegrityViolationException("duplicate", uniqueViolation))
                .given(userProfileRepository).saveAndFlush(any(UserProfile.class));

        assertThatThrownBy(() -> service().createProfile(1L, sampleRequest()))
                .isInstanceOf(UserProfileAlreadyExistsException.class);
    }

    @Test
    @DisplayName("길이 초과처럼 제약조건 위반이 아닌 DB 에러는 '이미 존재'로 바꾸지 않고 그대로 던진다")
    void createProfile_dataTooLong_rethrowsOriginalException() {
        User user = createUserWithId(1L);
        given(userProfileRepository.existsByUserId(1L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        // 실제 DB(Postgres 22001 value too long)처럼 Hibernate DataException을 원인으로 담아 던진다
        DataException tooLong = new DataException(
                "value too long", new SQLException("value too long for type character varying(255)", "22001"));
        willThrow(new DataIntegrityViolationException("value too long", tooLong))
                .given(userProfileRepository).saveAndFlush(any(UserProfile.class));

        assertThatThrownBy(() -> service().createProfile(1L, sampleRequest()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(UserProfileAlreadyExistsException.class);
    }
}
