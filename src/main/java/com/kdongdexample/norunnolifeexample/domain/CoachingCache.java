package com.kdongdexample.norunnolifeexample.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// AI 코칭 결과 캐시. 회원당 1행.
// 코칭 입력(최근 14일 컨디션 + 룰 판정)은 새 컨디션을 저장하기 전까지 같으므로
// 마지막 컨디션 id가 저장 당시와 같으면 LLM을 다시 부르지 않고 이 값을 돌려줍니다.
@Entity
@Table(name = "coaching_cache")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoachingCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "last_condition_id", nullable = false)
    private Long lastConditionId;

    // CoachingAdvice를 JSON 문자열로 저장
    @Column(name = "advice", nullable = false, columnDefinition = "text")
    private String advice;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private CoachingCache(Long userId, Long lastConditionId, String advice) {
        this.userId = userId;
        this.lastConditionId = lastConditionId;
        this.advice = advice;
    }

    public static CoachingCache create(Long userId, Long lastConditionId, String advice) {
        return new CoachingCache(userId, lastConditionId, advice);
    }

    public boolean isFor(Long lastConditionId) {
        return this.lastConditionId.equals(lastConditionId);
    }

    public void update(Long lastConditionId, String advice) {
        this.lastConditionId = lastConditionId;
        this.advice = advice;
    }
}
