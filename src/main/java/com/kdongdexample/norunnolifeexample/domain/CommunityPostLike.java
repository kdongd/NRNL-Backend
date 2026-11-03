
package com.kdongdexample.norunnolifeexample.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "community_post_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_community_post_like_post_user",
                columnNames = {"post_id", "user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostLike {

    // 좋아요 고유번호 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 좋아요 대상 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    // 좋아요를 누른 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private CommunityPostLike(CommunityPost post, User user) {
        this.post = post;
        this.user = user;
    }

    // 게시글 좋아요 생성
    public static CommunityPostLike create(
            CommunityPost post,
            User user
    ) {
        return new CommunityPostLike(post, user);
    }
}
