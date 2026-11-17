package com.kdongd.nrnl.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("create()로 만든 로컬 계정은 LOCAL provider와 비밀번호를 가진다")
    void create_setsLocalProviderAndPassword() {
        User user = User.create("test@test.com", "encoded-password");

        assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(user.getProviderId()).isNull();
        assertThat(user.hasPassword()).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("createOAuth()로 만든 구글 계정은 비밀번호가 없다")
    void createOAuth_googleAccountHasNoPassword() {
        User user = User.createOAuth("oauth@test.com", AuthProvider.GOOGLE, "google-sub-123");

        assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getProviderId()).isEqualTo("google-sub-123");
        assertThat(user.hasPassword()).isFalse();
    }
}
