package com.kdongd.nrnl.crew.repository;

import com.kdongd.nrnl.crew.domain.CrewApplication;
import com.kdongd.nrnl.crew.domain.CrewApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrewApplicationRepository extends JpaRepository<CrewApplication, Long> {

    Optional<CrewApplication> findByIdAndCrewId(Long id, Long crewId);

    boolean existsByCrewIdAndUserIdAndStatus(Long crewId, Long userId, CrewApplicationStatus status);

    Page<CrewApplication> findAllByCrewIdOrderByCreatedAtDescIdDesc(Long crewId, Pageable pageable);
}
