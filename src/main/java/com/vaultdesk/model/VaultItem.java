package com.vaultdesk.model;

import java.time.LocalDateTime;

public class VaultItem {

    public enum ItemType { CREDENTIAL, NOTE, DOCUMENT }

    private int itemId;
    private int userId;
    private String title;
    private String category;
    private ItemType itemType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    private LocalDateTime deletedAt;

    public VaultItem() {}

    public VaultItem(int itemId, int userId, String title, String category,
                      ItemType itemType, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.itemId = itemId;
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.itemType = itemType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
