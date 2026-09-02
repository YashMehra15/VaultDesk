package com.vaultdesk.model;

import java.time.LocalDateTime;

public class SecurityLog {
    private int logId;
    private int userId;
    private String action;
    private LocalDateTime timestamp;

    public SecurityLog() {}

    public SecurityLog(int logId, int userId, String action, LocalDateTime timestamp) {
        this.logId = logId;
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
