package com.vaultdesk.service;

import com.vaultdesk.model.User;

public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;
    private EncryptionService encryptionService;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void startSession(User user, EncryptionService encryptionService) {
        this.currentUser = user;
        this.encryptionService = encryptionService;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public EncryptionService getEncryptionService() {
        if (encryptionService == null) {
            throw new IllegalStateException("No active session - user must log in first");
        }
        return encryptionService;
    }

    public boolean isLoggedIn() {
        return currentUser != null && encryptionService != null;
    }

    public void lock() {
        this.currentUser = null;
        this.encryptionService = null;
    }
}
