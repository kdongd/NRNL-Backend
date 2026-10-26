
package com.kdongdexample.norunnolifeexample.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kdongdexample.norunnolifeexample.domain.AuthProvider;
import com.kdongdexample.norunnolifeexample.domain.ExperienceLevel;
import com.kdongdexample.norunnolifeexample.domain.Gender;
import com.kdongdexample.norunnolifeexample.domain.RefreshToken;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.domain.WorkoutType;
import com.kdongdexample.norunnolifeexample.dto.AuthTokens;
import com.kdongdexample.norunnolifeexample.dto.GoogleLoginRequest;
import com.kdongdexample.norunnolifeexample.dto.LoginRequest;
import com.kdongdexample.norunnolifeexample.dto.SignupRequest;
import com.kdongdexample.norunnolifeexample.dto.UserProfileRequest;
import com.kdongdexample.norunnolifeexample.exception.EmailAlreadyExistsException;
import com.kdongdexample.norunnolifeexample.exception.InvalidCredentialsException;
import com.kdongdexample.norunnolifeexample.exception.InvalidGoogleTokenException;
import com.kdongdexample.norunnolifeexample.exception.InvalidRefreshTokenException;
import com.kdongdexample.norunnolifeexample.repository.RefreshTokenRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.security.GoogleIdTokenValidator;
import com.kdongdexample.norunnolifeexample.security.JwtTokenProvider;
import com.kdongdexample.norunnolifeexample.security.RefreshTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1_209_600_000L;
    private static final int MAX_REFRESH_TOKENS_PER_USER = 5;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private GoogleIdTokenValidator googleIdTokenValidator;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenProvider refreshTokenProvider;

    @Mock
    private NicknameService nicknameService;

    @Mock
    private UserProfileService userProfileService;

    private AuthService service() {
        return new AuthService(userRepository, passwordEncoder, jwtTokenProvider, googleIdTokenValidator,
                transactionManager, refreshTokenRepository, refreshTokenProvider,
                REFRESH_TOKEN_EXPIRATION_MS, MAX_REFRESH_TOKENS_PER_USER,
                nicknameService, userProfileService);
    }

    private SignupRequest signupRequest(String email) {
        UserProfileRequest profile = new UserProfileRequest(
                Gender.MAN,
                28,
                175.0,
                70.0,
                ExperienceLevel.INTERMEDIATE,
                Set.of(WorkoutType.RUNNING),
                "10km 완주"
        );

        return new SignupRequest(email, "password1234", "테스트유저", profile);
    }

    private User createLocalUserWithId(Long id, String email, String encodedPassword) {
        User user = User.create(email, encodedPassword);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private User createGoogleUserWithId(Long id, String email, String googleUserId) {
        User user = User.createOAuth(email, AuthProvider.GOOGLE, googleUserId);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private GoogleIdToken.Payload verifiedPayload(String email, boolean emailVerified, String subject) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(email);
        payload.setEmailVerified(emailVerified);
        payload.setSubject(subject);
        return payload;
    }

    // ===== signup =====

    @Test
    @DisplayName("회원가입 성공 시 비밀번호를 인코딩해서 저장한다")
    void signup_success() {
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        given(passwordEncoder.encode("password1234")).willReturn("encoded-password");

        service().signup(signupRequest("new@test.com"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@test.com");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(saved.getNickname()).isEqualTo("테스트유저");

        verify(nicknameService).validateNickname("테스트유저");
        verify(userProfileService).createProfile(eq(saved), any(UserProfileRequest.class));
    }

    @Test
    @DisplayName("이미 가입된 이메일로 회원가입하면 EmailAlreadyExistsException이 발생하고 저장은 호출되지 않는다")
    void signup_emailAlreadyExists_throwsException() {
        given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> service().signup(signupRequest("dup@test.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    // ===== login =====

    @Test
    @DisplayName("이메일과 비밀번호가 일치하면 액세스 토큰과 리프레시 토큰을 발급한다")
    void login_success() {
        User user = createLocalUserWithId(1L, "user@test.com", "encoded-password");
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1234", "encoded-password")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L, "user@test.com")).willReturn("access-token");
        given(refreshTokenProvider.generate()).willReturn("raw-refresh-token");
        given(refreshTokenProvider.hash("raw-refresh-token")).willReturn("hashed-refresh-token");

        AuthTokens tokens = service().login(new LoginRequest("user@test.com", "password1234"));

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isEqualTo("raw-refresh-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 InvalidCredentialsException이 발생한다")
    void login_userNotFound_throwsInvalidCredentialsException() {
        given(userRepository.findByEmail("none@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service().login(new LoginRequest("none@test.com", "password1234")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("존재하지 않는 이메일이어도 타이밍 사이드채널 방지를 위해 matches()가 더미 해시로 호출된다")
    void login_userNotFound_stillCallsPasswordEncoderForTimingSafety() {
        given(userRepository.findByEmail("none@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service().login(new LoginRequest("none@test.com", "password1234")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder).matches(eq("password1234"), any());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 InvalidCredentialsException이 발생한다")
    void login_wrongPassword_throwsInvalidCredentialsException() {
        User user = createLocalUserWithId(1L, "user@test.com", "encoded-password");
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> service().login(new LoginRequest("user@test.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("비밀번호가 없는 OAuth 전용 계정으로 로그인 시도해도 타이밍 사이드채널 방지를 위해 matches()가 더미 해시로 호출되고, InvalidCredentialsException이 발생한다")
    void login_oauthOnlyAccount_stillCallsPasswordEncoderForTimingSafety_throwsInvalidCredentials() {
        User oauthUser = createGoogleUserWithId(1L, "oauth@test.com", "google-sub-1");
        given(userRepository.findByEmail("oauth@test.com")).willReturn(Optional.of(oauthUser));

        assertThatThrownBy(() -> service().login(new LoginRequest("oauth@test.com", "aaaaaaaa")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder).matches(eq("aaaaaaaa"), any());
    }

    @Test
    @DisplayName("활성 세션이 최대 개수에 도달한 상태에서 로그인하면 가장 오래된 세션이 자동 폐기된다")
    void login_deviceLimitReached_evictsOldestSession() {
        User user = createLocalUserWithId(1L, "user@test.com", "encoded-password");
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1234", "encoded-password")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L, "user@test.com")).willReturn("access-token");
        given(refreshTokenProvider.generate()).willReturn("raw-refresh-token");
        given(refreshTokenProvider.hash("raw-refresh-token")).willReturn("hashed-refresh-token");

        RefreshToken oldest = RefreshToken.issue(1L, "hash-1", "family-1", LocalDateTime.now().plusDays(1));
        List<RefreshToken> activeTokens = new ArrayList<>(List.of(
                oldest,
                RefreshToken.issue(1L, "hash-2", "family-2", LocalDateTime.now().plusDays(1)),
                RefreshToken.issue(1L, "hash-3", "family-3", LocalDateTime.now().plusDays(1)),
                RefreshToken.issue(1L, "hash-4", "family-4", LocalDateTime.now().plusDays(1)),
                RefreshToken.issue(1L, "hash-5", "family-5", LocalDateTime.now().plusDays(1))
        ));
        given(refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfterOrderByIssuedAtAsc(eq(1L), any(LocalDateTime.class)))
                .willReturn(activeTokens);

        service().login(new LoginRequest("user@test.com", "password1234"));

        assertThat(oldest.isRevoked()).isTrue();
        assertThat(activeTokens.get(1).isRevoked()).isFalse();
    }

    // ===== loginWithGoogle =====

    @Test
    @DisplayName("가입 이력이 없는 이메일로 구글 로그인하면 신규 계정을 생성하고 토큰을 발급한다")
    void loginWithGoogle_newUser_createsAccountAndReturnsToken() {
        GoogleIdToken.Payload payload = verifiedPayload("new-google@test.com", true, "google-sub-2");
        given(googleIdTokenValidator.verify("valid-id-token")).willReturn(payload);
        given(userRepository.findByEmail("new-google@test.com")).willReturn(Optional.empty());
        User created = createGoogleUserWithId(2L, "new-google@test.com", "google-sub-2");
        given(userRepository.save(any(User.class))).willReturn(created);
        given(jwtTokenProvider.createAccessToken(2L, "new-google@test.com")).willReturn("google-access-token");
        given(refreshTokenProvider.generate()).willReturn("raw-refresh-token");
        given(refreshTokenProvider.hash("raw-refresh-token")).willReturn("hashed-refresh-token");

        AuthTokens tokens = service().loginWithGoogle(new GoogleLoginRequest("valid-id-token"));

        assertThat(tokens.accessToken()).isEqualTo("google-access-token");
        assertThat(tokens.refreshToken()).isEqualTo("raw-refresh-token");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(captor.getValue().getProviderId()).isEqualTo("google-sub-2");

        verify(transactionManager).getTransaction(any());
        verify(transactionManager).commit(any());
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 신규 계정을 만들지 않고 기존 계정으로 로그인 처리한다 (자동 연결, provider는 갱신하지 않음)")
    void loginWithGoogle_existingUser_doesNotCreateNewAccountOrChangeProvider() {
        GoogleIdToken.Payload payload = verifiedPayload("existing@test.com", true, "google-sub-3");
        given(googleIdTokenValidator.verify("valid-id-token")).willReturn(payload);
        User existing = createLocalUserWithId(3L, "existing@test.com", "encoded-password");
        given(userRepository.findByEmail("existing@test.com")).willReturn(Optional.of(existing));
        given(jwtTokenProvider.createAccessToken(3L, "existing@test.com")).willReturn("existing-access-token");
        given(refreshTokenProvider.generate()).willReturn("raw-refresh-token");
        given(refreshTokenProvider.hash("raw-refresh-token")).willReturn("hashed-refresh-token");

        AuthTokens tokens = service().loginWithGoogle(new GoogleLoginRequest("valid-id-token"));

        assertThat(tokens.accessToken()).isEqualTo("existing-access-token");
        verify(userRepository, never()).save(any());
        assertThat(existing.getProvider()).isEqualTo(AuthProvider.LOCAL);
    }

    @Test
    @DisplayName("이메일이 인증되지 않은 구글 토큰이면 InvalidGoogleTokenException이 발생하고 트랜잭션이 시작되지 않는다")
    void loginWithGoogle_emailNotVerified_throwsInvalidGoogleTokenException() {
        GoogleIdToken.Payload payload = verifiedPayload("unverified@test.com", false, "google-sub-4");
        given(googleIdTokenValidator.verify("valid-id-token")).willReturn(payload);

        assertThatThrownBy(() -> service().loginWithGoogle(new GoogleLoginRequest("valid-id-token")))
                .isInstanceOf(InvalidGoogleTokenException.class);

        verify(userRepository, never()).findByEmail(any());
        verify(transactionManager, never()).getTransaction(any());
    }

    @Test
    @DisplayName("구글 토큰 검증기가 예외를 던지면 그대로 전파되고 트랜잭션이 시작되지 않는다")
    void loginWithGoogle_invalidToken_propagatesExceptionWithoutStartingTransaction() {
        given(googleIdTokenValidator.verify("invalid-token")).willThrow(new InvalidGoogleTokenException());

        assertThatThrownBy(() -> service().loginWithGoogle(new GoogleLoginRequest("invalid-token")))
                .isInstanceOf(InvalidGoogleTokenException.class);

        verify(userRepository, never()).findByEmail(any());
        verify(transactionManager, never()).getTransaction(any());
    }

    // ===== refresh =====

    @Test
    @DisplayName("유효한 리프레시 토큰으로 재발급하면 기존 토큰은 폐기되고 같은 family로 새 토큰이 발급된다")
    void refresh_success_rotatesTokenAndReturnsNewTokens() {
        RefreshToken existing = RefreshToken.issue(1L, "old-hash", "family-A", LocalDateTime.now().plusDays(1));
        given(refreshTokenProvider.hash("raw-old-token")).willReturn("old-hash");
        given(refreshTokenRepository.findByTokenHashForUpdate("old-hash")).willReturn(Optional.of(existing));
        given(userRepository.findById(1L))
                .willReturn(Optional.of(createLocalUserWithId(1L, "user@test.com", "encoded-password")));
        given(jwtTokenProvider.createAccessToken(1L, "user@test.com")).willReturn("new-access-token");
        given(refreshTokenProvider.generate()).willReturn("raw-new-token");
        given(refreshTokenProvider.hash("raw-new-token")).willReturn("new-hash");

        AuthTokens tokens = service().refresh("raw-old-token");

        assertThat(tokens.accessToken()).isEqualTo("new-access-token");
        assertThat(tokens.refreshToken()).isEqualTo("raw-new-token");
        assertThat(existing.isRevoked()).isTrue();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenFamily()).isEqualTo("family-A");
    }

    @Test
    @DisplayName("이미 폐기된(회전에 사용된) 토큰이 재사용되면 같은 family 전체를 폐기하고 예외를 던진다")
    void refresh_reusedRevokedToken_revokesEntireFamilyAndThrows() {
        RefreshToken revoked = RefreshToken.issue(1L, "revoked-hash", "family-A", LocalDateTime.now().plusDays(1));
        revoked.revoke();
        given(refreshTokenProvider.hash("stolen-token")).willReturn("revoked-hash");
        given(refreshTokenRepository.findByTokenHashForUpdate("revoked-hash")).willReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service().refresh("stolen-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).revokeAllByTokenFamily("family-A");
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로 재발급 시도하면 예외가 발생하고 family 전체 폐기는 호출되지 않는다")
    void refresh_expiredToken_throwsWithoutFamilyRevoke() {
        RefreshToken expired = RefreshToken.issue(1L, "expired-hash", "family-A", LocalDateTime.now().minusDays(1));
        given(refreshTokenProvider.hash("expired-token")).willReturn("expired-hash");
        given(refreshTokenRepository.findByTokenHashForUpdate("expired-hash")).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> service().refresh("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).revokeAllByTokenFamily(any());
    }

    @Test
    @DisplayName("존재하지 않는 리프레시 토큰이면 InvalidRefreshTokenException이 발생한다")
    void refresh_unknownToken_throwsInvalidRefreshTokenException() {
        given(refreshTokenProvider.hash("unknown-token")).willReturn("unknown-hash");
        given(refreshTokenRepository.findByTokenHashForUpdate("unknown-hash")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service().refresh("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    // ===== logout =====

    @Test
    @DisplayName("로그아웃 시 해당 리프레시 토큰을 폐기한다")
    void logout_revokesMatchingToken() {
        RefreshToken token = RefreshToken.issue(1L, "hash", "family-A", LocalDateTime.now().plusDays(1));
        given(refreshTokenProvider.hash("raw-token")).willReturn("hash");
        given(refreshTokenRepository.findByTokenHash("hash")).willReturn(Optional.of(token));

        service().logout("raw-token");

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 로그아웃해도 예외 없이 조용히 끝난다")
    void logout_unknownToken_doesNotThrow() {
        given(refreshTokenProvider.hash("unknown")).willReturn("unknown-hash");
        given(refreshTokenRepository.findByTokenHash("unknown-hash")).willReturn(Optional.empty());

        assertThatCode(() -> service().logout("unknown")).doesNotThrowAnyException();
    }
}
