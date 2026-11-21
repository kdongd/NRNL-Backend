package com.kdongd.nrnl.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.kdongd.nrnl.exception.GoogleVerificationUnavailableException;
import com.kdongd.nrnl.exception.InvalidGoogleTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Slf4j
@Component
public class GoogleIdTokenValidator {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenValidator(@Value("${google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (IllegalArgumentException e) {
            // 토큰 문자열 자체가 JWT 형식이 아님 -> 클라이언트가 보낸 값의 문제, 401 유지
            throw new InvalidGoogleTokenException();
        } catch (GeneralSecurityException | IOException e) {
            // 서명 검증 과정(구글 공개키 서버 조회 등)에서 나는 인프라/네트워크 문제.
            // 클라이언트의 토큰이 잘못된 게 아니라 우리 서버가 검증을 못 끝낸 것이므로
            // 401이 아니라 5xx로 응답하고, 원인 추적을 위해 로그를 남긴다.
            log.error("구글 ID 토큰 검증 실패 - 외부 인프라(구글 공개키 서버 등) 문제로 추정", e);
            throw new GoogleVerificationUnavailableException();
        }

        if (idToken == null) {
            throw new InvalidGoogleTokenException();
        }

        return idToken.getPayload();
    }
}
