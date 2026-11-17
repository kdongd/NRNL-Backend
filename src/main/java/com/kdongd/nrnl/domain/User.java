package com.kdongd.nrnl.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_provider_provider_id",
                columnNames = {"provider", "provider_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // OAuth 전용 계정은 비밀번호가 없음 -> nullable
    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "nickname_normalized", unique = true)
    private String nicknameNormalized;

    private User(String email, String password, UserRole role, AuthProvider provider, String providerId) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
    }

    // 기존 시그니처 그대로 유지 -> 이 메서드를 쓰는 기존 코드/테스트(AuthService, Workout* 테스트 전부) 전혀 안 건드려도 됨
    public static User create(String email, String encodedPassword) {
        return new User(email, encodedPassword, UserRole.USER, AuthProvider.LOCAL, null);
    }

    public static User createOAuth(String email, AuthProvider provider, String providerId) {
        return new User(email, null, UserRole.USER, provider, providerId);
    }

    public boolean hasPassword() {
        return password != null;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
        this.nicknameNormalized = nickname.toLowerCase();
    }
}
