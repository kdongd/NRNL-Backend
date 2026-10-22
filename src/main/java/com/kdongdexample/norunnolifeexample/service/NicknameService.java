package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.dto.NicknameAvailabilityResponse;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.InvalidNicknameException;
import com.kdongdexample.norunnolifeexample.exception.NicknameAlreadyExistsException;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.validation.NicknameValidator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class NicknameService {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 10;

    private final UserRepository userRepository;
    private final NicknameValidator nicknameValidator;

    public NicknameService(
            UserRepository userRepository,
            NicknameValidator nicknameValidator
    ) {
        this.userRepository = userRepository;
        this.nicknameValidator = nicknameValidator;
    }

    public NicknameAvailabilityResponse checkAvailability(String nickname) {
        if (!isValidFormat(nickname)) {
            return new NicknameAvailabilityResponse(false, "INVALID_FORMAT");
        }
        if (nicknameValidator.isBanned(nickname)) {
            return new NicknameAvailabilityResponse(false, "RESERVED");
        }
        if (userRepository.existsByNicknameNormalized(nickname.toLowerCase())) {
            return new NicknameAvailabilityResponse(false, "DUPLICATE");
        }
        return new NicknameAvailabilityResponse(true, null);
    }

    @Transactional
    public void changeNickname(Long userId, String nickname) {
        if (!isValidFormat(nickname)) {
            throw new InvalidNicknameException("닉네임은 2~10자여야 합니다");
        }
        if (nicknameValidator.isBanned(nickname)) {
            throw new InvalidNicknameException("사용할 수 없는 닉네임입니다");
        }
        if (userRepository.existsByNicknameNormalized(normalize(nickname))) {
            throw new NicknameAlreadyExistsException("이미 사용 중인 닉네임입니다");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(userId));
        user.updateNickname(nickname);
    }


    @Transactional(readOnly = true)
    public String getNickname(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(userId));

        return user.getNickname();
    }



    private boolean isValidFormat(String nickname) {
            if (nickname == null) return false;
            int length = nickname.length();
            return length >= MIN_LENGTH && length <= MAX_LENGTH;
        }

    private String normalize(String nickname) {
        return nickname.toLowerCase();
    }
}
