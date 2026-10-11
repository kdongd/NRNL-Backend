package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.AuthProvider;
import com.kdongdexample.norunnolifeexample.domain.RefreshToken;
import com.kdongdexample.norunnolifeexample.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class RefreshTokenRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    @DisplayName("deleteAllExpiredOrRevoked는 만료되었거나 폐기된 토큰만 삭제하고 유효한 토큰은 남긴다")
    void deleteAllExpiredOrRevoked_removesOnlyExpiredOrRevokedTokens() {
        User user = userRepository.save(
                User.createOAuth("cleanup-test@test.com", AuthProvider.GOOGLE, "google-sub-cleanup"));

        refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), "expired-hash", UUID.randomUUID().toString(),
                        LocalDateTime.now().minusDays(1)));

        String revokedFamily = UUID.randomUUID().toString();
        refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), "revoked-hash", revokedFamily,
                        LocalDateTime.now().plusDays(1)));
        refreshTokenRepository.revokeAllByTokenFamily(revokedFamily);

        RefreshToken validToken = refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), "valid-hash", UUID.randomUUID().toString(),
                        LocalDateTime.now().plusDays(1)));

        int deletedCount = refreshTokenRepository.deleteAllExpiredOrRevoked(LocalDateTime.now());

        assertThat(deletedCount).isEqualTo(2);
        List<RefreshToken> remaining = refreshTokenRepository.findAll();
        assertThat(remaining).extracting(RefreshToken::getId)
                .containsExactly(validToken.getId());
    }

    @Test
    @Transactional
    @DisplayName("findByUserIdAndRevokedFalseAndExpiresAtAfterOrderByIssuedAtAsc는 만료/폐기된 토큰을 제외하고 발급순으로 반환한다")
    void findActiveTokens_excludesExpiredAndRevoked_orderedByIssuedAt() throws InterruptedException {
        User user = userRepository.save(
                User.createOAuth("device-limit-test@test.com", AuthProvider.GOOGLE, "google-sub-device-limit"));

        RefreshToken oldest = refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), "oldest-hash", UUID.randomUUID().toString(),
                        LocalDateTime.now().plusDays(1)));
        Thread.sleep(10);
        RefreshToken newest = refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), "newest-hash", UUID.randomUUID().toString(),
                        LocalDateTime.now().plusDays(1)));

        String revokedFamily = UUID.randomUUID().toString();
        refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), "revoked-hash", revokedFamily,
                        LocalDateTime.now().plusDays(1)));
        refreshTokenRepository.revokeAllByTokenFamily(revokedFamily);

        refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), "expired-hash", UUID.randomUUID().toString(),
                        LocalDateTime.now().minusDays(1)));

        List<RefreshToken> active = refreshTokenRepository
                .findByUserIdAndRevokedFalseAndExpiresAtAfterOrderByIssuedAtAsc(user.getId(), LocalDateTime.now());

        assertThat(active).extracting(RefreshToken::getId)
                .containsExactly(oldest.getId(), newest.getId());
    }
}
