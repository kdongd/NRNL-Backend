package com.kdongd.nrnl.crew.repository;

import com.kdongd.nrnl.domain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CrewUserRepository extends Repository<User, Long> {

    // 사용자 단위 정책을 직렬화하되 다른 크루의 FK 참조와 충돌하지 않는다.
    @Query(value = "select * from users where id = :id for no key update", nativeQuery = true)
    Optional<User> findForUpdate(@Param("id") Long id);
}
