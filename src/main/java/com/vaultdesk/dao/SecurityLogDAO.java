package com.vaultdesk.dao;

import com.vaultdesk.database.DatabaseConnection;
import com.vaultdesk.model.SecurityLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SecurityLogDAO {

    public void record(int userId, String action) {
        String sql = "INSERT INTO security_logs (user_id, action) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to record security log: " + e.getMessage());
        }
    }

    public List<SecurityLog> findRecentByUser(int userId, int limit) {
        String sql = "SELECT * FROM security_logs WHERE user_id = ? ORDER BY timestamp DESC LIMIT ?";
        List<SecurityLog> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SecurityLog log = new SecurityLog();
                    log.setLogId(rs.getInt("log_id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setAction(rs.getString("action"));
                    Timestamp ts = rs.getTimestamp("timestamp");
                    if (ts != null) log.setTimestamp(ts.toLocalDateTime());
                    results.add(log);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch security logs", e);
        }
        return results;
    }
}
