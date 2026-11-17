package com.kdongd.nrnl.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class RefreshTokenProvider {

    private static final int TOKEN_BYTE_LENGTH = 64;

    private final SecureRandom secureRandom = new SecureRandom();

    // 클라이언트에게 내려줄 원문 리프레시 토큰 생성 (64바이트 = 512비트 랜덤값)
    public String generate() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // 원문 토큰을 SHA-256으로 해시 (DB엔 이 값만 저장)
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 JDK 표준에 항상 포함되는 알고리즘이라 실제로는 발생 불가능한 예외.
            // 실제로는 발생할 수 없는 예외지만 getInstance(String)는 체크 예외로 선언되어 있어서
            // 그대로 놔두면 이 메서드를 호출하는 모든곳에서 억지로 try-catch 하거나 throws를 달아야 합니다.
            // 그걸 막기 위해서 여기서 한번 잡고 언체크 예외로 바꿔서 다시 던집니다.
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }
}
