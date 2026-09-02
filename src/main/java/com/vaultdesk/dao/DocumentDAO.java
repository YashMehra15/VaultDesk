package com.vaultdesk.dao;

import com.vaultdesk.database.DatabaseConnection;
import com.vaultdesk.model.Document;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentDAO {

    public Document create(int itemId, String fileName, String filePath, String fileHash) {
        String sql = "INSERT INTO documents (item_id, file_name, file_path, file_hash) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itemId);
            ps.setString(2, fileName);
            ps.setString(3, filePath);
            ps.setString(4, fileHash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Document(keys.getInt(1), itemId, fileName, filePath, fileHash);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create document record", e);
        }
    }

    public Optional<Document> findByItemId(int itemId) {
        String sql = "SELECT * FROM documents WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Document(
                            rs.getInt("document_id"),
                            rs.getInt("item_id"),
                            rs.getString("file_name"),
                            rs.getString("file_path"),
                            rs.getString("file_hash")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch document", e);
        }
        return Optional.empty();
    }

    public List<Document> findAllByUser(int userId) {
        String sql = "SELECT d.* FROM documents d " +
                "JOIN vault_items v ON d.item_id = v.item_id " +
                "WHERE v.user_id = ? AND v.is_deleted = 0 ORDER BY v.updated_at DESC";
        List<Document> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new Document(
                            rs.getInt("document_id"),
                            rs.getInt("item_id"),
                            rs.getString("file_name"),
                            rs.getString("file_path"),
                            rs.getString("file_hash")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch documents", e);
        }
        return results;
    }
}
