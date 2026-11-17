package com.kdongd.nrnl.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS 설정은 SecurityConfig.corsConfigurationSource()로 이동함.
    // (Security 필터체인이 MVC보다 먼저 요청을 가로채므로, 인증 실패 응답에도
    //  CORS 헤더가 붙으려면 Security 레벨에서 CORS를 처리해야 함)
}
