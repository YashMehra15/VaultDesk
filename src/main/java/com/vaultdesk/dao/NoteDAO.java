package com.vaultdesk.dao;

import com.vaultdesk.database.DatabaseConnection;
import com.vaultdesk.model.SecureNote;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteDAO {

    public SecureNote create(int itemId, String encryptedContent) {
        String sql = "INSERT INTO secure_notes (item_id, encrypted_content) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itemId);
            ps.setString(2, encryptedContent);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getInt(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create note", e);
        }
    }

    public Optional<SecureNote> findById(int noteId) {
        String sql = "SELECT * FROM secure_notes WHERE note_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch note", e);
        }
        return Optional.empty();
    }

    public List<SecureNote> findAllByUser(int userId) {
        String sql = "SELECT n.* FROM secure_notes n " +
                "JOIN vault_items v ON n.item_id = v.item_id " +
                "WHERE v.user_id = ? AND v.is_deleted = 0 ORDER BY v.updated_at DESC";
        List<SecureNote> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch notes", e);
        }
        return results;
    }

    public void update(int noteId, String encryptedContent) {
        String sql = "UPDATE secure_notes SET encrypted_content = ? WHERE note_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, encryptedContent);
            ps.setInt(2, noteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update note", e);
        }
    }

    private SecureNote map(ResultSet rs) throws SQLException {
        return new SecureNote(
                rs.getInt("note_id"),
                rs.getInt("item_id"),
                rs.getString("encrypted_content")
        );
    }
}
