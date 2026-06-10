package org.example.config.security;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Verifies credentials (bcrypt) and issues a JWT. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResult login(String email, String password) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, password_hash, display_name, status FROM users WHERE email = ?", email);
        if (rows.isEmpty()) {
            throw new BadCredentialsException("Invalid email or password");
        }
        Map<String, Object> row = rows.get(0);
        if (!"ACTIVE".equals(row.get("status"))) {
            throw new BadCredentialsException("Account is disabled");
        }
        if (!passwordEncoder.matches(password, (String) row.get("password_hash"))) {
            throw new BadCredentialsException("Invalid email or password");
        }
        UUID userId = (UUID) row.get("id");
        String token = jwtService.issue(userId, email);
        return new LoginResult(token, userId, (String) row.get("display_name"));
    }

    public record LoginResult(String token, UUID userId, String displayName) {
    }
}
