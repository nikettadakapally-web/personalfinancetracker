package com.anurag.cse;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final LoginSessionRepository sessions;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AppUserRepository users, LoginSessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    public AppUser register(String name, String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        AppUser user = new AppUser();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        return users.save(user);
    }

    public AppUser authenticate(String email, String password) {
        AppUser user = users.findByEmailIgnoreCase(email.trim()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        }
        return user;
    }

    public String createSession(AppUser user) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new LoginSession(token, user, Instant.now().plus(7, ChronoUnit.DAYS)));
        return token;
    }

    public AppUser requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        String token = authorization.substring(7).trim();
        Optional<LoginSession> session = sessions.findByTokenAndExpiresAtAfter(token, Instant.now());
        return session.map(LoginSession::getUser).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your session has expired. Please sign in again."));
    }

    public void revokeSession(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            sessions.deleteById(authorization.substring(7).trim());
        }
    }
}