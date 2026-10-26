
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.domain.UserProfile;
import com.kdongdexample.norunnolifeexample.dto.UserProfileRequest;
import com.kdongdexample.norunnolifeexample.dto.UserProfileResponse;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.UserProfileAlreadyExistsException;
import com.kdongdexample.norunnolifeexample.exception.UserProfileNotFoundException;
import com.kdongdexample.norunnolifeexample.repository.CoachingCacheRepository;
import com.kdongdexample.norunnolifeexample.repository.UserProfileRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CoachingCacheRepository coachingCacheRepository;

    public UserProfileService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            CoachingCacheRepository coachingCacheRepository
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.coachingCacheRepository = coachingCacheRepository;
    }

    // 기존 API: 최초 운동 프로필 등록
    @Transactional
    public UserProfile createProfile(
            Long userId,
            UserProfileRequest request
    ) {
        if (userProfileRepository.existsByUserId(userId)) {
            throw new UserProfileAlreadyExistsException(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new AuthenticatedUserNotFoundException(userId)
                );

        return createProfile(user, request);
    }

    // 회원가입 API에서 사용하는 프로필 생성
    @Transactional
    public UserProfile createProfile(
            User user,
            UserProfileRequest request
    ) {
        if (userProfileRepository.existsByUserId(user.getId())) {
            throw new UserProfileAlreadyExistsException(user.getId());
        }

        UserProfile profile = UserProfile.create(
                user,
                request.gender(),
                request.age(),
                request.heightCm(),
                request.weightKg(),
                request.experienceLevel(),
                request.primaryExercises(),
                request.goal()
        );

        // existsByUserId()와 save() 사이에 동시 요청이 끼어들면
        // 두 요청 모두 중복 검사를 통과할 수 있다.
        // DB UNIQUE 제약조건이 중복 저장을 최종적으로 방지하며,
        // saveAndFlush()로 제약조건 위반을 즉시 확인한다.
        try {
            return userProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof
                    org.hibernate.exception.ConstraintViolationException) {
                throw new UserProfileAlreadyExistsException(user.getId());
            }
            throw e;
        }
    }

    // [추가] 로그인한 사용자의 운동 프로필 조회
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new UserProfileNotFoundException(userId)
                );

        // 트랜잭션 안에서 DTO로 변환해 지연 로딩 문제 방지
        return UserProfileResponse.from(profile);
    }

    // [추가] 로그인한 사용자의 운동 프로필 전체 수정
    @Transactional
    public UserProfileResponse updateProfile(
            Long userId,
            UserProfileRequest request
    ) {
        // 프로필 존재 여부를 먼저 확인한다.
        userProfileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new UserProfileNotFoundException(userId)
                );

        // 기존 AI 코칭 캐시 제거
        // deleteByUserId()에 clearAutomatically=true가 설정되어 있으므로
        // 영속성 컨텍스트가 초기화된 후 프로필을 다시 조회한다.
        coachingCacheRepository.deleteByUserId(userId);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new UserProfileNotFoundException(userId)
                );

        profile.update(
                request.gender(),
                request.age(),
                request.heightCm(),
                request.weightKg(),
                request.experienceLevel(),
                request.primaryExercises(),
                request.goal()
        );

        // 영속 상태이므로 별도의 save() 없이 변경 감지로 UPDATE된다.
        // 응답의 수정 시각까지 반영하기 위해 flush한다.
        userProfileRepository.flush();

        return UserProfileResponse.from(profile);
    }
}
