
package com.kdongdexample.norunnolifeexample.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "community_comment_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_community_comment_like_comment_user",
                columnNames = {"comment_id", "user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityCommentLike {

    // 좋아요 고유번호 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 좋아요 대상 댓글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private CommunityComment comment;

    // 좋아요를 누른 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private CommunityCommentLike(
            CommunityComment comment,
            User user
    ) {
        this.comment = comment;
        this.user = user;
    }

    // 댓글 좋아요 생성
    public static CommunityCommentLike create(
            CommunityComment comment,
            User user
    ) {
        return new CommunityCommentLike(comment, user);
    }
}
