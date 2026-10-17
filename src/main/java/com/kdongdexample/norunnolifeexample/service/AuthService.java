package com.kdongdexample.norunnolifeexample.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kdongdexample.norunnolifeexample.domain.AuthProvider;
import com.kdongdexample.norunnolifeexample.domain.RefreshToken;
import com.kdongdexample.norunnolifeexample.domain.User;
import com.kdongdexample.norunnolifeexample.dto.AuthTokens;
import com.kdongdexample.norunnolifeexample.dto.GoogleLoginRequest;
import com.kdongdexample.norunnolifeexample.dto.LoginRequest;
import com.kdongdexample.norunnolifeexample.dto.SignupRequest;
import com.kdongdexample.norunnolifeexample.exception.AuthenticatedUserNotFoundException;
import com.kdongdexample.norunnolifeexample.exception.EmailAlreadyExistsException;
import com.kdongdexample.norunnolifeexample.exception.InvalidCredentialsException;
import com.kdongdexample.norunnolifeexample.exception.InvalidGoogleTokenException;
import com.kdongdexample.norunnolifeexample.exception.InvalidRefreshTokenException;
import com.kdongdexample.norunnolifeexample.repository.RefreshTokenRepository;
import com.kdongdexample.norunnolifeexample.repository.UserRepository;
import com.kdongdexample.norunnolifeexample.security.GoogleIdTokenValidator;
import com.kdongdexample.norunnolifeexample.security.JwtTokenProvider;
import com.kdongdexample.norunnolifeexample.security.RefreshTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthService {

    // 계정이 없거나(OAuth 전용 계정 포함) 비밀번호 자체가 없는 경우에도 matches()를
    // 동일하게 한 번 호출시켜서 걸리는 시간을 맞추기 위한 더미 해시.
    // 실제 어떤 계정의 비밀번호도 아니고, BCrypt 연산 시간을 채우는 용도로만 쓴다.
    private static final String DUMMY_PASSWORD_HASH =
            "$2b$10$VNV8QWPKZdi5BrXTpnILDeHthOkTomdS2EezISxvZ1M37S3k.l..q";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleIdTokenValidator googleIdTokenValidator;
    private final TransactionTemplate transactionTemplate;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProvider refreshTokenProvider;
    private final long refreshTokenExpirationMs;
    private final int maxRefreshTokensPerUser;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       GoogleIdTokenValidator googleIdTokenValidator,
                       PlatformTransactionManager transactionManager,
                       RefreshTokenRepository refreshTokenRepository,
                       RefreshTokenProvider refreshTokenProvider,
                       @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
                       @Value("${auth.max-refresh-tokens-per-user}") int maxRefreshTokensPerUser) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleIdTokenValidator = googleIdTokenValidator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenProvider = refreshTokenProvider;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.maxRefreshTokensPerUser = maxRefreshTokensPerUser;
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        User user = User.create(request.email(), passwordEncoder.encode(request.password()));

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                throw new EmailAlreadyExistsException(request.email());
            }
            throw e;   // 길이 초과 같은 건 그대로 올려보낸다
        }
    }

    @Transactional
    public AuthTokens login(LoginRequest request) {
        Optional<User> maybeUser = userRepository.findByEmail(request.email());

        // 계정이 없거나(Optional.empty) OAuth 전용 계정(비밀번호 없음)이어도 더미 해시로
        // matches()를 동일하게 호출해서, "계정이 있고 비밀번호만 틀린 경우"와 걸리는 시간을
        // 맞춘다. 그렇지 않으면 메시지는 통일해놔도 응답 시간만으로 "이 이메일이 LOCAL
        // 계정으로 존재하는지"가 새어나간다(타이밍 사이드채널).
        String hashToCheck = maybeUser
                .filter(User::hasPassword)
                .map(User::getPassword)
                .orElse(DUMMY_PASSWORD_HASH);

        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (maybeUser.isEmpty() || !maybeUser.get().hasPassword() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        User user = maybeUser.get();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = issueRefreshToken(user.getId(), UUID.randomUUID().toString());

        return new AuthTokens(accessToken, refreshToken);
    }

    // 클래스 레벨 @Transactional(readOnly = true)을 이 메서드가 그대로 물려받으면, 아래
    // transactionTemplate.execute()가 기본 전파(REQUIRED)일 때 그 readOnly 트랜잭션에 합류해버려서
    // 신규 유저 저장(INSERT)이 read-only 커넥션 위에서 거부된다 (Post /auth/google 자체가 죽는 버그였음).
    // NOT_SUPPORTED로 이 메서드 진입 시점엔 트랜잭션을 아예 열지 않게 만든다 — 어차피
    // googleIdTokenValidator.verify()는 구글 공개키 서버 조회 등 외부 네트워크 호출이 걸릴 수 있어서
    // 이 구간을 트랜잭션(=DB 커넥션 점유)에 묶어둘 이유가 없다. DB 조회/저장이 필요한 구간만
    // 아래 transactionTemplate.execute()가 그 자리에서 새 트랜잭션을 연다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthTokens loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = googleIdTokenValidator.verify(request.idToken());

        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new InvalidGoogleTokenException();
        }

        String email = payload.getEmail();
        String googleUserId = payload.getSubject();

        return transactionTemplate.execute(status -> {
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> userRepository.save(User.createOAuth(email, AuthProvider.GOOGLE, googleUserId)));

            String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
            String refreshToken = issueRefreshToken(user.getId(), UUID.randomUUID().toString());

            return new AuthTokens(accessToken, refreshToken);
        });
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokens refresh(String rawRefreshToken) {
        String tokenHash = refreshTokenProvider.hash(rawRefreshToken);

        // 동시 요청으로 인해 두 요청이 모두 revoked=false 상태를 읽어 재사용 탐지 무력화를 방지합니다.
        // findByTokenHashForUpdate(PESSIMISTIC_WRITE)를 사용해 동일 토큰의 동시 접근시 나중 요청을 대기시키고
        // 첫번째 트랜잭션이 완료된 후 최신 revoked 상태를 조회하도록 보장합니다.
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            // 폐기된 토큰이 다시 들어오는 경우 = 탈취되어 재사용된 것으로 간주합니다.
            // 같은 tokenFamily(같은 세션/기기에서 나온 토큰들) 전체를 즉시 폐기해서
            // 피해 범위를 그 세션 하나로 한정합니다. 다른 기기(다른 family)에는 영향이 가지 않습니다.
            log.warn("리프레시 토큰 재사용 탐지 - 해당 세션(tokenFamily={}, userId={}) 전체를 폐기합니다.",
                    current.getTokenFamily(), current.getUserId());
            refreshTokenRepository.revokeAllByTokenFamily(current.getTokenFamily());
            throw new InvalidRefreshTokenException();
        }

        if (current.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        // 지금 쓰인 토큰은 즉시 폐기하고 같은 family로 새 토큰을 발급합니다.
        current.revoke();

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(current.getUserId()));

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = issueRefreshToken(user.getId(), current.getTokenFamily());

        return new AuthTokens(accessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = refreshTokenProvider.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(RefreshToken::revoke);
        // 토큰이 없거나 이미 폐기된 상태여도 에러를 내지 않습니다.
    }

    private String issueRefreshToken(Long userId, String tokenFamily) {
        enforceDeviceLimit(userId);

        String rawToken = refreshTokenProvider.generate();
        String tokenHash = refreshTokenProvider.hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs));

        RefreshToken refreshToken = RefreshToken.issue(userId, tokenHash, tokenFamily, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private void enforceDeviceLimit(Long userId) {
        // revokedFalse만 보고 만료 여부를 안 보면 만료된 지 오래된 토큰까지 활성 슬롯을
        // 차지해서 진짜 활성 세션이 부당하게 먼저 밀려날 수 있어서, 아직 만료 안 된
        // 토큰만 세도록 expiresAt 조건이 들어간 쿼리로 교체했습니다.
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findByUserIdAndRevokedFalseAndExpiresAtAfterOrderByIssuedAtAsc(userId, LocalDateTime.now());
        if (activeTokens.size() < maxRefreshTokensPerUser) {
            return;
        }
        int excess = activeTokens.size() - maxRefreshTokensPerUser + 1;
        for (int i = 0; i < excess; i++) {
            activeTokens.get(i).revoke();
        }
        // activeTokens는 이 트랜잭션 안에서 조회된 영속 상태 엔티티라서 revoke() 호출만으로도
        // 트랜잭션 시 변경된 부분이 자동으로 반영됩니다.
    }
}
