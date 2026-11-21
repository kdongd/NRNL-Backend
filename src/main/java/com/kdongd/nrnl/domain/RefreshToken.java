package com.kdongd.nrnl.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash")
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 보안 강화를 위해 원문 토큰이 아니라 SHA-256 해시만 저장 (DB 유출 시에도 그대로 재사용 불가하게 하기 위함)
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    // 회전(Rotation) 체인 식별자. 최초 로그인 시 생성되고 이후 재발급마다 그대로 이어받습니다.
    // 토큰 재사용 탐지 시 이 값이 같은 row들만 한번에 폐기해서 "그 세션(기기)만" 끊습니다.
    @Column(name = "token_family", nullable = false, length = 36)
    private String tokenFamily;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @CreatedDate
    @Column(name = "issued_at", updatable = false)
    private LocalDateTime issuedAt;

    private RefreshToken(Long userId, String tokenHash, String tokenFamily, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.tokenFamily = tokenFamily;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public static RefreshToken issue(Long userId, String tokenHash, String tokenFamily, LocalDateTime expiresAt) {
        return new RefreshToken(userId, tokenHash, tokenFamily, expiresAt);
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUsable() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }
}
