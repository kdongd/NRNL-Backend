package com.kdongd.nrnl.crew.domain;

import com.kdongd.nrnl.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long crewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CrewApplicationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    public static CrewApplication create(Long crewId, User user, Instant createdAt) {
        CrewApplication application = new CrewApplication();
        application.crewId = crewId;
        application.user = user;
        application.status = CrewApplicationStatus.PENDING;
        application.createdAt = createdAt;
        return application;
    }

    public void resolve(CrewApplicationStatus status, Instant resolvedAt) {
        this.status = status;
        this.resolvedAt = resolvedAt;
    }
}
