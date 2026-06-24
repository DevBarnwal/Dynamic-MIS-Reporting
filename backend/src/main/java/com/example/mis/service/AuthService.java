package com.example.mis.service;

import com.example.mis.dto.CurrentUser;
import com.example.mis.dto.LoginResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final NamedParameterJdbcTemplate jdbc;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LoginResponse login(String username, String password) {
        CurrentUser user = findUserByCredentials(username, hashPassword(password));
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        jdbc.update("""
                INSERT INTO user_session (token, user_id, expires_at)
                VALUES (:token, :userId, :expiresAt)
                """, Map.of(
                "token", token,
                "userId", user.userId(),
                "expiresAt", LocalDateTime.now().plusHours(8)
        ));

        return new LoginResponse(token, user);
    }

    public CurrentUser requireUser(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return findUserByToken(authorizationHeader.substring("Bearer ".length()));
    }

    public CurrentUser findUserByToken(String token) {
        try {
            return jdbc.queryForObject("""
                    SELECT u.user_id, u.username, u.full_name, r.role_code,
                           u.department_id, u.course_id, u.student_id
                    FROM user_session s
                    JOIN app_user u ON u.user_id = s.user_id
                    JOIN role r ON r.role_id = u.role_id
                    WHERE s.token = :token
                      AND s.expires_at > CURRENT_TIMESTAMP
                      AND u.active = TRUE
                    """, Map.of("token", token), (rs, rowNum) -> new CurrentUser(
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role_code"),
                    readNullableLong(rs, "department_id"),
                    readNullableLong(rs, "course_id"),
                    readNullableLong(rs, "student_id")
            ));
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired login");
        }
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jdbc.update("DELETE FROM user_session WHERE token = :token", Map.of("token", authorizationHeader.substring("Bearer ".length())));
        }
    }

    private CurrentUser findUserByCredentials(String username, String passwordHash) {
        try {
            return jdbc.queryForObject("""
                    SELECT u.user_id, u.username, u.full_name, r.role_code,
                           u.department_id, u.course_id, u.student_id
                    FROM app_user u
                    JOIN role r ON r.role_id = u.role_id
                    WHERE LOWER(u.username) = LOWER(:username)
                      AND u.password_hash = :passwordHash
                      AND u.active = TRUE
                    """, Map.of("username", username, "passwordHash", passwordHash), (rs, rowNum) -> new CurrentUser(
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role_code"),
                    readNullableLong(rs, "department_id"),
                    readNullableLong(rs, "course_id"),
                    readNullableLong(rs, "student_id")
            ));
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash password", ex);
        }
    }

    private Long readNullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
