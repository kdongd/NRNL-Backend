package com.kdongdexample.norunnolifeexample.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

// 서비스의 "오늘" 기준 존이 JVM 설정(-Duser.timezone)과 무관하게 Asia/Seoul인지 검증한다.
// ClockConfig가 systemDefaultZone()으로 돌아가면 UTC JVM(CI, 운영)에서 이 테스트가 깨진다.
class ClockConfigTest {

    @Test
    @DisplayName("서비스 Clock은 JVM 타임존과 무관하게 Asia/Seoul이다")
    void clock_isAlwaysSeoul() {
        assertThat(new ClockConfig().clock().getZone())
                .isEqualTo(ZoneId.of("Asia/Seoul"));
    }
}
