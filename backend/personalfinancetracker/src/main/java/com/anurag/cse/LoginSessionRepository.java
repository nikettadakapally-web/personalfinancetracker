package com.anurag.cse;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSessionRepository extends JpaRepository<LoginSession, String> {
    Optional<LoginSession> findByTokenAndExpiresAtAfter(String token, java.time.Instant now);
}