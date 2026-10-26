
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
import com.kdongdexample.norunnolifeexample.exception.NicknameAlreadyExistsException;
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

    // [추가] 회원가입에서 닉네임 검증 및 운동 프로필 생성을 재사용
    private final NicknameService nicknameService;
    private final UserProfileService userProfileService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       GoogleIdTokenValidator googleIdTokenValidator,
                       PlatformTransactionManager transactionManager,
                       RefreshTokenRepository refreshTokenRepository,
                       RefreshTokenProvider refreshTokenProvider,
                       @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
                       @Value("${auth.max-refresh-tokens-per-user}") int maxRefreshTokensPerUser,
                       NicknameService nicknameService,
                       UserProfileService userProfileService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleIdTokenValidator = googleIdTokenValidator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenProvider = refreshTokenProvider;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.maxRefreshTokensPerUser = maxRefreshTokensPerUser;

        // [추가]
        this.nicknameService = nicknameService;
        this.userProfileService = userProfileService;
    }

    // [수정] 회원, 닉네임, 운동 프로필을 하나의 트랜잭션으로 저장
    @Transactional
    public void signup(SignupRequest request) {

        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // 2. 닉네임 형식, 금칙어, 중복 검사
        nicknameService.validateNickname(request.nickname());

        // 3. 사용자 생성
        User user = User.create(
                request.email(),
                passwordEncoder.encode(request.password())
        );

        // 4. 닉네임 설정
        user.updateNickname(request.nickname());

        // 5. 사용자 저장
        // DB UNIQUE 제약조건이 동시 가입 요청의 중복을 최종적으로 방지한다.
        // saveAndFlush()로 INSERT 시점에 제약조건 위반을 확인한다.
        // 이메일뿐 아니라 닉네임도 UNIQUE이므로 모든 제약조건 위반을
        // EmailAlreadyExistsException으로 변환하면 안 된다.
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            Throwable cause = e;

            while (cause != null) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintException) {
                    String constraintName = constraintException.getConstraintName();

                    if ("uk_users_email".equalsIgnoreCase(constraintName)) {
                        throw new EmailAlreadyExistsException(request.email());
                    }

                    if ("uk_users_nickname_normalized".equalsIgnoreCase(constraintName)) {
                        throw new NicknameAlreadyExistsException("이미 사용 중인 닉네임입니다");
                    }
                }

                cause = cause.getCause();
            }

            throw e;
        }

        // 6. 운동 프로필 생성
        // UserProfileService는 별도 빈이므로 @Transactional 프록시를 거친다.
        // 기본 전파(REQUIRED)에 따라 현재 signup() 트랜잭션에 참여한다.
        userProfileService.createProfile(user, request.profile());

        // 모든 저장이 성공하면 COMMIT
        // 중간에 RuntimeException이 발생하면 전체 ROLLBACK
    }

    @Transactional
    public AuthTokens login(LoginRequest request) {
        Optional<User> maybeUser = userRepository.findByEmail(request.email());

        // 계정이 없거나(Optional.empty()) OAuth 전용 계정(비밀번호 없음)이어도
        // 더미 해시로 matches()를 동일하게 호출해 타이밍 차이를 줄인다.
        String hashToCheck = maybeUser
                .filter(User::hasPassword)
                .map(User::getPassword)
                .orElse(DUMMY_PASSWORD_HASH);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                hashToCheck
        );

        if (maybeUser.isEmpty()
                || !maybeUser.get().hasPassword()
                || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        User user = maybeUser.get();

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail()
        );

        String refreshToken = issueRefreshToken(
                user.getId(),
                UUID.randomUUID().toString()
        );

        return new AuthTokens(accessToken, refreshToken);
    }

    // 클래스 레벨 @Transactional(readOnly = true)을 이 메서드가 그대로 물려받으면,
    // transactionTemplate.execute()가 기본 전파(REQUIRED)로
    // readOnly 트랜잭션에 합류할 수 있다.
    // Google 토큰 검증은 외부 네트워크 호출이 발생할 수 있으므로
    // DB 트랜잭션 밖에서 처리하고, DB 작업만 별도 트랜잭션으로 묶는다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthTokens loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = googleIdTokenValidator.verify(
                request.idToken()
        );

        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new InvalidGoogleTokenException();
        }

        String email = payload.getEmail();
        String googleUserId = payload.getSubject();

        return transactionTemplate.execute(status -> {
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> userRepository.save(
                            User.createOAuth(
                                    email,
                                    AuthProvider.GOOGLE,
                                    googleUserId
                            )
                    ));

            String accessToken = jwtTokenProvider.createAccessToken(
                    user.getId(),
                    user.getEmail()
            );

            String refreshToken = issueRefreshToken(
                    user.getId(),
                    UUID.randomUUID().toString()
            );

            return new AuthTokens(accessToken, refreshToken);
        });
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokens refresh(String rawRefreshToken) {
        String tokenHash = refreshTokenProvider.hash(rawRefreshToken);

        // 동일 토큰에 대한 동시 요청을 직렬화해 재사용 탐지를 보호한다.
        RefreshToken current = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            // 폐기된 토큰이 다시 사용되면 동일 세션의 토큰을 모두 폐기한다.
            log.warn(
                    "리프레시 토큰 재사용 탐지 - 해당 세션(tokenFamily={}, userId={}) 전체를 폐기합니다.",
                    current.getTokenFamily(),
                    current.getUserId()
            );

            refreshTokenRepository.revokeAllByTokenFamily(
                    current.getTokenFamily()
            );

            throw new InvalidRefreshTokenException();
        }

        if (current.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        // 기존 리프레시 토큰 폐기
        current.revoke();

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() ->
                        new AuthenticatedUserNotFoundException(
                                current.getUserId()
                        )
                );

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail()
        );

        String newRefreshToken = issueRefreshToken(
                user.getId(),
                current.getTokenFamily()
        );

        return new AuthTokens(accessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = refreshTokenProvider.hash(rawRefreshToken);

        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(RefreshToken::revoke);

        // 토큰이 없거나 이미 폐기된 상태여도 에러를 내지 않는다.
    }

    private String issueRefreshToken(Long userId, String tokenFamily) {
        enforceDeviceLimit(userId);

        String rawToken = refreshTokenProvider.generate();
        String tokenHash = refreshTokenProvider.hash(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now()
                .plus(Duration.ofMillis(refreshTokenExpirationMs));

        RefreshToken refreshToken = RefreshToken.issue(
                userId,
                tokenHash,
                tokenFamily,
                expiresAt
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private void enforceDeviceLimit(Long userId) {
        // 만료되지 않았고 revoked=false인 토큰만 활성 토큰으로 계산한다.
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findByUserIdAndRevokedFalseAndExpiresAtAfterOrderByIssuedAtAsc(
                        userId,
                        LocalDateTime.now()
                );

        if (activeTokens.size() < maxRefreshTokensPerUser) {
            return;
        }

        int excess = activeTokens.size() - maxRefreshTokensPerUser + 1;

        for (int i = 0; i < excess; i++) {
            activeTokens.get(i).revoke();
        }

        // 조회된 RefreshToken은 영속 상태이므로
        // revoke() 변경 사항은 트랜잭션 종료 시 반영된다.
    }
}
