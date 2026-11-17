package com.kdongd.nrnl.scheduler;

import com.kdongd.nrnl.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 만료됐거나 이미 폐기된 리프레시 토큰 row를 주기적으로 삭제합니다.
@Slf4j
@Component
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // 매일 새벽 4시에 트래픽이 거의 없는 시간대에 돌려 부하를 피합니다.
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteAllExpiredOrRevoked(LocalDateTime.now());
        log.info("만료/폐기된 리프레시 토큰 {}건 정리 완료", deleted);
    }
}
