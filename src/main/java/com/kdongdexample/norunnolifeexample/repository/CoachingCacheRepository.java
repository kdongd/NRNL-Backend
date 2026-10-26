package com.kdongdexample.norunnolifeexample.repository;

import com.kdongdexample.norunnolifeexample.domain.CoachingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CoachingCacheRepository extends JpaRepository<CoachingCache, Long> {

    Optional<CoachingCache> findByUserId(Long userId);

    // 컨디션이 바뀌면 그 회원의 코칭 캐시를 지운다.
    @Modifying(clearAutomatically = true)
    @Query("delete from CoachingCache c where c.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
