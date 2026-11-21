package com.kdongd.nrnl.crew.domain;

import com.kdongd.nrnl.domain.WorkoutType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Crew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long leaderId;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @ElementCollection
    @CollectionTable(name = "crew_sport", joinColumns = @JoinColumn(name = "crew_id"))
    @Column(name = "sport", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 32)
    private Set<WorkoutType> sports = new HashSet<>();

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CrewStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static Crew create(Long leaderId, String name, String description,
                              Set<WorkoutType> sports, int capacity, Instant createdAt) {
        Crew crew = new Crew();
        crew.leaderId = leaderId;
        crew.createdAt = createdAt;
        crew.update(name, description, sports, capacity, CrewStatus.OPEN);
        return crew;
    }

    public void update(String name, String description, Set<WorkoutType> sports,
                       int capacity, CrewStatus status) {
        this.name = name;
        this.description = description;
        this.sports.retainAll(sports);
        this.sports.addAll(sports);
        this.capacity = capacity;
        this.status = status;
    }

    public void transferLeadership(Long leaderId) {
        this.leaderId = leaderId;
    }
}
