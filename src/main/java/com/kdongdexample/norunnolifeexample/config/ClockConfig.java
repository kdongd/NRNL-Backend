package com.kdongdexample.norunnolifeexample.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;


@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        // 한국 사용자 전용 서비스. 서버가 어디서 어떤 타임존으로 뜨든
        // 이 서비스의 "오늘"은 항상 Asia/Seoul 기준이다.
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
