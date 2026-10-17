package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.domain.UserProfile;
import com.kdongdexample.norunnolifeexample.dto.UserProfileRequest;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.UserProfileAlreadyExistsException;
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

    public UserProfileService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfile createProfile(Long userId, UserProfileRequest request) {
        if (userProfileRepository.existsByUserId(userId)) {
            throw new UserProfileAlreadyExistsException(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(userId));

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

        // existsByUserId 체크와 save() 사이에 동시 요청이 끼어들면 둘 다 체크를 통과하고
        // signup()처럼 user_id 유니크 제약 위반이 그대로 터질 수 있습니다.
        // saveAndFlush로 즉시 반영시켜서 제약 위반을 여기서 잡아 예외로 변환 합니다.
        try {
            return userProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                throw new UserProfileAlreadyExistsException(userId);
            }
            throw e;   // 길이 초과 같은 건 그대로 올려보낸다
        }
    }
}
