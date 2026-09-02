package com.vaultdesk.dao;

import com.vaultdesk.database.DatabaseConnection;
import com.vaultdesk.model.VaultItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VaultItemDAO {

    public VaultItem create(int userId, String title, String category, VaultItem.ItemType type) {
        String sql = "INSERT INTO vault_items (user_id, title, category, item_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, category);
            ps.setString(4, type.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getInt(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create vault item", e);
        }
    }

    public Optional<VaultItem> findById(int itemId) {
        String sql = "SELECT * FROM vault_items WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch vault item", e);
        }
        return Optional.empty();
    }

    public List<VaultItem> findByUserAndType(int userId, VaultItem.ItemType type) {
        String sql = "SELECT * FROM vault_items WHERE user_id = ? AND item_type = ? AND is_deleted = 0 ORDER BY updated_at DESC";
        List<VaultItem> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch vault items", e);
        }
        return results;
    }

    public Map<Integer, VaultItem> findAllByUserAsMap(int userId) {
        String sql = "SELECT * FROM vault_items WHERE user_id = ? AND is_deleted = 0";
        Map<Integer, VaultItem> results = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VaultItem item = map(rs);
                    results.put(item.getItemId(), item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch vault items", e);
        }
        return results;
    }

    public List<VaultItem> findDeletedByUser(int userId) {
        String sql = "SELECT * FROM vault_items WHERE user_id = ? AND is_deleted = 1 ORDER BY deleted_at DESC";
        List<VaultItem> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch trash items", e);
        }
        return results;
    }

    public void touchUpdatedAt(int itemId) {
        String sql = "UPDATE vault_items SET updated_at = CURRENT_TIMESTAMP WHERE item_id = ?";
        exec(sql, itemId);
    }

    /** Soft delete - moves the item to Trash instead of removing it. */
    public void delete(int itemId) {
        String sql = "UPDATE vault_items SET is_deleted = 1, deleted_at = CURRENT_TIMESTAMP WHERE item_id = ?";
        exec(sql, itemId);
    }

    /** Restores an item out of Trash. */
    public void restore(int itemId) {
        String sql = "UPDATE vault_items SET is_deleted = 0, deleted_at = NULL WHERE item_id = ?";
        exec(sql, itemId);
    }

    /** Permanently removes an item (and, via cascade, its credential/note/document row). */
    public void permanentlyDelete(int itemId) {
        String sql = "DELETE FROM vault_items WHERE item_id = ?";
        exec(sql, itemId);
    }

    private void exec(String sql, int itemId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update vault item", e);
        }
    }

    private VaultItem map(ResultSet rs) throws SQLException {
        VaultItem item = new VaultItem();
        item.setItemId(rs.getInt("item_id"));
        item.setUserId(rs.getInt("user_id"));
        item.setTitle(rs.getString("title"));
        item.setCategory(rs.getString("category"));
        item.setItemType(VaultItem.ItemType.valueOf(rs.getString("item_type")));
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        if (created != null) item.setCreatedAt(created.toLocalDateTime());
        if (updated != null) item.setUpdatedAt(updated.toLocalDateTime());
        item.setDeleted(rs.getInt("is_deleted") == 1);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) item.setDeletedAt(deletedAt.toLocalDateTime());
        return item;
    }
}
