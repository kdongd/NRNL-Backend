package com.kdongd.nrnl.repository;

import com.kdongd.nrnl.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // refresh() 전용 락 조회. 같은 토큰으로 동시 요청이 들어와도 두번째 요청이
    // 첫번째 트랜잭션이 끝날 때까지 대기하게 만들어서 RTR 재사용 탐지가 무력화되지 않게 합니다.
    // logout()처럼 단순 폐기만 하는 곳까지 락을 걸 이유는 없는것 같아서 별도 메서드로 분리 했습니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    // 사용 가능한(폐기되지 않았고 아직 만료도 안 된) 세션만 오래된 순으로 조회
    // -> 기기 수 제한 시 가장 오래된 것부터 정리합니다.
    // revokedFalse만 보고 만료 여부를 안보면 만료된지 오래된 토큰까지 슬롯을
    // 차지해서 진짜 활성 세션이 먼저 밀려날 수 있어서 expiresAt 조건을 추가했습니다.
    List<RefreshToken> findByUserIdAndRevokedFalseAndExpiresAtAfterOrderByIssuedAtAsc(Long userId, LocalDateTime now);

    // 재사용 탐지 시 같은 체인을 전체 폐기합니다.
    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.tokenFamily = :tokenFamily")
    void revokeAllByTokenFamily(@Param("tokenFamily") String tokenFamily);

    // 만료됐거나 이미 폐기된 토큰은 주기적으로 삭제합니다.
    // 안해도 기능상 문제는 없지만 테이블 용량 증가와 인덱스 성능 저하를 막을 수 있습니다.
    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :now or r.revoked = true")
    int deleteAllExpiredOrRevoked(@Param("now") LocalDateTime now);
}
