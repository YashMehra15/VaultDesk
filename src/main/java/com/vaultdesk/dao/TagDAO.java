package com.vaultdesk.dao;

import com.vaultdesk.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages free-form tags and their many-to-many link to vault items.
 * Powers both tag display (e.g. "#github, #work") and tag-based search
 * ("tag:github") in the Passwords and Notes tabs.
 */
public class TagDAO {

    private int findOrCreateTag(Connection conn, int userId, String name) throws SQLException {
        String select = "SELECT tag_id FROM tags WHERE user_id = ? AND name = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("tag_id");
            }
        }
        String insert = "INSERT INTO tags (user_id, name) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Replaces all tags on an item with the given set (case-insensitive, deduplicated). */
    public void setTagsForItem(int userId, int itemId, List<String> tagNames) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement clear = conn.prepareStatement("DELETE FROM item_tags WHERE item_id = ?")) {
                clear.setInt(1, itemId);
                clear.executeUpdate();
            }
            Set<String> clean = new LinkedHashSet<>();
            for (String raw : tagNames) {
                String t = raw.trim().toLowerCase();
                if (!t.isEmpty()) clean.add(t);
            }
            for (String tagName : clean) {
                int tagId = findOrCreateTag(conn, userId, tagName);
                try (PreparedStatement link = conn.prepareStatement(
                        "INSERT OR IGNORE INTO item_tags (item_id, tag_id) VALUES (?, ?)")) {
                    link.setInt(1, itemId);
                    link.setInt(2, tagId);
                    link.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update tags", e);
        }
    }

    public List<String> getTagsForItem(int itemId) {
        String sql = "SELECT t.name FROM tags t " +
                "JOIN item_tags it ON t.tag_id = it.tag_id " +
                "WHERE it.item_id = ? ORDER BY t.name";
        List<String> tags = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) tags.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch tags", e);
        }
        return tags;
    }

    /** Item IDs (for this user) that carry the given tag - used by tag: search. */
    public List<Integer> findItemIdsByTag(int userId, String tagName) {
        String sql = "SELECT it.item_id FROM item_tags it " +
                "JOIN tags t ON it.tag_id = t.tag_id " +
                "WHERE t.user_id = ? AND t.name = ?";
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, tagName.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("item_id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search by tag", e);
        }
        return ids;
    }
}
