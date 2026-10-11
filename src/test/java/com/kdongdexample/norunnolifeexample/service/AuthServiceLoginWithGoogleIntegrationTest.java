package com.kdongdexample.norunnolifeexample.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kdongdexample.norunnolifeexample.domain.AuthProvider;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.dto.AuthTokens;
import com.kdongdexample.norunnolifeexample.dto.GoogleLoginRequest;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.security.GoogleIdTokenValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

// Mock 기반의 단위 테스트는 Spring AOP 프록시를 거치지 않아, loginWithGoogle() 실행 시
// 클래스 레벨의 @Transactional(readOnly = true) 설정으로 인해 신규 유저 저장(INSERT)이
// Read-only 커넥션에서 거부되던 생산 환경 버그를 포착할 수 없었습니다.
// 이를 방지하기 위해 실제 Spring 컨테이너와 Testcontainers 기반의 PostgreSQL 환경을 구성하여
// 트랜잭션 프록시가 개입한 상태에서도 신규 유저가 정상적으로 DB에 영속화되는지 검증합니다.
// PostgreSQL JDBC 드라이버도 read-only 트랜잭션에서는 SET TRANSACTION READ ONLY를 적용해
// INSERT를 거부하므로, 운영 DB와 동일한 환경에서 회귀 테스트를 수행합니다.
@Testcontainers
@SpringBootTest
class AuthServiceLoginWithGoogleIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    // 구글 서버로 실제 네트워크 요청이 나가지 않도록 검증 로직만 목 처리.
    // DB/트랜잭션 관련된 빈은 전부 실제 빈을 그대로 사용합니다.
    @MockitoBean
    private GoogleIdTokenValidator googleIdTokenValidator;

    @Test
    @DisplayName("신규 구글 유저 로그인 시 실제 DB에 유저가 저장된다 (readOnly 트랜잭션 합류 버그 회귀 테스트)")
    void loginWithGoogle_newUser_actuallyPersistsUserToRealDatabase() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("new-google-user@test.com");
        payload.setEmailVerified(true);
        payload.setSubject("google-sub-real-db");
        given(googleIdTokenValidator.verify("valid-id-token")).willReturn(payload);

        AuthTokens tokens = authService.loginWithGoogle(new GoogleLoginRequest("valid-id-token"));

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        Optional<User> saved = userRepository.findByEmail("new-google-user@test.com");
        assertThat(saved).isPresent();
        assertThat(saved.get().getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(saved.get().getProviderId()).isEqualTo("google-sub-real-db");
    }
}
