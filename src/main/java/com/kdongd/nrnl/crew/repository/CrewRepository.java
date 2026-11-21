package com.kdongd.nrnl.crew.repository;

import com.kdongd.nrnl.crew.domain.Crew;
import com.kdongd.nrnl.crew.domain.CrewStatus;
import com.kdongd.nrnl.domain.WorkoutType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CrewRepository extends JpaRepository<Crew, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Crew c where c.id = :id")
    Optional<Crew> findForUpdate(@Param("id") Long id);

    boolean existsByLeaderId(Long leaderId);

    @Query(value = """
            select c as crew,
                   (select count(m) from CrewMember m where m.crewId = c.id) as memberCount
            from Crew c
            where locate(:name, translate(c.name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')) > 0
              and (:anySport = true or :sport member of c.sports)
              and c.status = coalesce(:status, c.status)
            order by c.createdAt desc, c.id desc
            """, countQuery = """
            select count(c) from Crew c
            where locate(:name, translate(c.name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')) > 0
              and (:anySport = true or :sport member of c.sports)
              and c.status = coalesce(:status, c.status)
            """)
    Page<CrewSummary> search(@Param("name") String name,
                             @Param("sport") WorkoutType sport,
                             @Param("anySport") boolean anySport,
                             @Param("status") CrewStatus status,
                             Pageable pageable);

    interface CrewSummary {
        Crew getCrew();

        long getMemberCount();
    }
}
