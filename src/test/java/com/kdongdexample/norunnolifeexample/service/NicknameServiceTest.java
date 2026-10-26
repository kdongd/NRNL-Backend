
package com.kdongdexample.norunnolifeexample.service;

import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.exception.NicknameAlreadyExistsException;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.validation.NicknameValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NicknameServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NicknameValidator nicknameValidator;

    @InjectMocks
    private NicknameService nicknameService;

    @Test
    @DisplayName("정상적인 닉네임으로 변경하면 성공한다")
    void changeNickname_success() {
        Long userId = 1L;
        String nickname = "Runner";

        User user = mock(User.class);

        when(nicknameValidator.isBanned(nickname))
                .thenReturn(false);

        when(userRepository.existsByNicknameNormalized("runner"))
                .thenReturn(false);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        nicknameService.changeNickname(userId, nickname);

        verify(user).updateNickname(nickname);
    }

    @Test
    @DisplayName("이미 동일한 닉네임이 존재하면 중복 예외가 발생한다")
    void changeNickname_duplicate_sameCase() {
        Long userId = 1L;
        String nickname = "runner";

        User user = mock(User.class);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(nicknameValidator.isBanned(nickname))
                .thenReturn(false);

        when(userRepository.existsByNicknameNormalized("runner"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                nicknameService.changeNickname(userId, nickname)
        )
                .isInstanceOf(NicknameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("대소문자만 다른 닉네임도 정규화 후 중복으로 처리한다")
    void changeNickname_duplicate_differentCase() {
        Long userId = 1L;
        String nickname = "RUNNER";

        User user = mock(User.class);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(nicknameValidator.isBanned(nickname))
                .thenReturn(false);

        when(userRepository.existsByNicknameNormalized("runner"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                nicknameService.changeNickname(userId, nickname)
        )
                .isInstanceOf(NicknameAlreadyExistsException.class);
    }
}
