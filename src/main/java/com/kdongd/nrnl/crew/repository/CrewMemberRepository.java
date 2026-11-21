package com.kdongd.nrnl.crew.repository;

import com.kdongd.nrnl.crew.domain.CrewMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    long countByCrewId(Long crewId);

    long countByUserId(Long userId);

    boolean existsByCrewIdAndUserId(Long crewId, Long userId);

    Optional<CrewMember> findByCrewIdAndUserId(Long crewId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<CrewMember> findAllByCrewIdOrderById(Long crewId);
}
