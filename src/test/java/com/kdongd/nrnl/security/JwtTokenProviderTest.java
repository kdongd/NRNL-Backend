package com.kdongd.nrnl.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-jwt-secret-for-unit-test-32bytes-minimum!!";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtTokenProvider provider() {
        return new JwtTokenProvider(SECRET, ONE_HOUR_MS);
    }

    @Test
    @DisplayName("발급한 토큰은 유효하고, userId를 그대로 복원한다")
    void createAndParse_roundTrip() {
        JwtTokenProvider provider = provider();
        String token = provider.createAccessToken(42L, "user@test.com");

        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않다")
    void expiredToken_isInvalid() {
        // 만료 시간을 음수로 줘서 발급 즉시 만료된 토큰을 만든다 (sleep 없이 결정적으로 재현)
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, -1000L);
        String token = provider.createAccessToken(1L, "user@test.com");

        assertThat(provider.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된(혹은 검증하는) 토큰은 유효하지 않다")
    void tokenSignedWithDifferentSecret_isInvalid() {
        JwtTokenProvider issuer = new JwtTokenProvider(SECRET, ONE_HOUR_MS);
        JwtTokenProvider verifier = new JwtTokenProvider("completely-different-secret-value-32bytes+", ONE_HOUR_MS);

        String token = issuer.createAccessToken(1L, "user@test.com");

        assertThat(verifier.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("시크릿에 비-ASCII 문자가 있어도 같은 문자열이면 같은 키로 동작한다 (charset 고정의 불변조건 문서화)")
    void nonAsciiSecret_sameStringProducesSameKey() {
        // 주의: 이 테스트는 같은 JVM(=같은 기본 인코딩) 안에서 돌기 때문에,
        // getBytes(UTF_8)를 getBytes()로 되돌려도 CI에서는 여전히 통과할 수 있다.
        // "크로스 환경 버그 재현"이 아니라 "같은 문자열 -> 같은 키"라는 불변조건을 문서화하는 목적.
        String nonAsciiSecret = "이건-테스트용-한글-포함-시크릿-값-32바이트이상되게길게씀!!";

        JwtTokenProvider issuer = new JwtTokenProvider(nonAsciiSecret, ONE_HOUR_MS);
        JwtTokenProvider verifier = new JwtTokenProvider(nonAsciiSecret, ONE_HOUR_MS);

        String token = issuer.createAccessToken(7L, "user@test.com");

        assertThat(verifier.isValid(token)).isTrue();
        assertThat(verifier.getUserId(token)).isEqualTo(7L);
    }
}
