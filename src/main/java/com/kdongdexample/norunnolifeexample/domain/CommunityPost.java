
package com.kdongdexample.norunnolifeexample.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "community_post")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost {

    // 게시글 고유번호 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 게시글 작성자 (다대일 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    // 게시글 제목 (최대 50자)
    @Column(nullable = false, length = 50)
    private String title;

    // 게시글 본문 (PostgreSQL TEXT)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 최초 작성 시간 (자동 기록)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 마지막 수정 시간 (자동 갱신)
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;


    private CommunityPost(User author, String title, String content) {
        this.author = author;
        this.title = title;
        this.content = content;
    }

    // 게시글 생성용 정적 팩토리 메서드
    public static CommunityPost create(User author, String title, String content) {
        return new CommunityPost(author, title, content);
    }

    // 게시글 수정
    // 작성자 변경 없이 제목과 본문만 수정
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
