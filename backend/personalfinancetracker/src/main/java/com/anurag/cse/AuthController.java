package com.anurag.cse;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRequest request) {
        validate(request, true);
        AppUser user = authService.register(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionResponse(user));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest request) {
        validate(request, false);
        return sessionResponse(authService.authenticate(request.email(), request.password()));
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return userResponse(authService.requireUser(authorization));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.revokeSession(authorization);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> sessionResponse(AppUser user) {
        return Map.of("token", authService.createSession(user), "user", userResponse(user));
    }

    private Map<String, Object> userResponse(AppUser user) {
        return Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail());
    }

    private void validate(AuthRequest request, boolean registration) {
        if (request == null || request.email() == null || request.password() == null
                || !EMAIL_PATTERN.matcher(request.email().trim()).matches()
                || request.email().trim().length() > 254 || request.password().length() < 10
                || request.password().getBytes(StandardCharsets.UTF_8).length > 72
                || (registration && (request.name() == null || request.name().trim().isEmpty()
                        || request.name().trim().length() > 100))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a valid email, a password of at least 10 characters and no more than 72 UTF-8 bytes, and a name of up to 100 characters when registering.");
        }
    }

    public record AuthRequest(String name, String email, String password) {}
}