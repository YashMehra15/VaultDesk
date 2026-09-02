package com.vaultdesk.dao;

import com.vaultdesk.database.DatabaseConnection;
import com.vaultdesk.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserDAO {

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up user", e);
        }
        return Optional.empty();
    }

    public User createUser(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            return findByUsername(username).orElseThrow();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create user", e);
        }
    }

    public void incrementFailedAttempts(int userId) {
        String sql = "UPDATE users SET failed_attempts = failed_attempts + 1 WHERE user_id = ?";
        exec(sql, userId);
    }

    public void resetFailedAttempts(int userId) {
        String sql = "UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE user_id = ?";
        exec(sql, userId);
    }

    public void lockUntil(int userId, LocalDateTime until) {
        String sql = "UPDATE users SET locked_until = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, until.toString());
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to lock account", e);
        }
    }

    public void updateMfaSecret(int userId, String secret) {
        String sql = "UPDATE users SET mfa_secret = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, secret);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update MFA secret", e);
        }
    }

    public void setMfaEnabled(int userId, boolean enabled) {
        String sql = "UPDATE users SET mfa_enabled = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enabled ? 1 : 0);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update MFA status", e);
        }
    }

    private void exec(String sql, int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        u.setFailedAttempts(rs.getInt("failed_attempts"));
        String lockedUntilStr = rs.getString("locked_until");
        if (lockedUntilStr != null && !lockedUntilStr.isBlank()) {
            try {
                u.setLockedUntil(java.time.LocalDateTime.parse(lockedUntilStr));
            } catch (Exception ignored) { /* leave null if unparsable */ }
        }
        u.setMfaSecret(rs.getString("mfa_secret"));
        u.setMfaEnabled(rs.getInt("mfa_enabled") == 1);
        return u;
    }
}
