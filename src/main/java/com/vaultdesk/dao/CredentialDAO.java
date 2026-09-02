package com.vaultdesk.dao;

import com.vaultdesk.database.DatabaseConnection;
import com.vaultdesk.model.Credential;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CredentialDAO {

    public Credential create(int itemId, String website, String username, String encryptedPassword) {
        String sql = "INSERT INTO credentials (item_id, website, username, encrypted_password) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itemId);
            ps.setString(2, website);
            ps.setString(3, username);
            ps.setString(4, encryptedPassword);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getInt(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create credential", e);
        }
    }

    public Optional<Credential> findById(int credentialId) {
        String sql = "SELECT * FROM credentials WHERE credential_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, credentialId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch credential", e);
        }
        return Optional.empty();
    }

    public Optional<Credential> findByItemId(int itemId) {
        String sql = "SELECT * FROM credentials WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch credential", e);
        }
        return Optional.empty();
    }

    public List<Credential> findAllByUser(int userId) {
        String sql = "SELECT c.* FROM credentials c " +
                "JOIN vault_items v ON c.item_id = v.item_id " +
                "WHERE v.user_id = ? AND v.is_deleted = 0 ORDER BY v.updated_at DESC";
        List<Credential> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch credentials", e);
        }
        return results;
    }

    public void update(int credentialId, String website, String username, String encryptedPassword) {
        String sql = "UPDATE credentials SET website = ?, username = ?, encrypted_password = ? WHERE credential_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, website);
            ps.setString(2, username);
            ps.setString(3, encryptedPassword);
            ps.setInt(4, credentialId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update credential", e);
        }
    }

    private Credential map(ResultSet rs) throws SQLException {
        return new Credential(
                rs.getInt("credential_id"),
                rs.getInt("item_id"),
                rs.getString("website"),
                rs.getString("username"),
                rs.getString("encrypted_password")
        );
    }
}
