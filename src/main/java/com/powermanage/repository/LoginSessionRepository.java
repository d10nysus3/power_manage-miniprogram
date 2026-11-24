package com.powermanage.repository;

import com.powermanage.entity.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<LoginSession> findByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    @Query("DELETE FROM LoginSession s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
