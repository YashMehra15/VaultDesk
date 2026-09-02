package com.vaultdesk.model;

public class Credential {
    private int credentialId;
    private int itemId;
    private String website;
    private String username;
    private String encryptedPassword;
    private transient String plainPassword;

    public Credential() {}

    public Credential(int credentialId, int itemId, String website, String username, String encryptedPassword) {
        this.credentialId = credentialId;
        this.itemId = itemId;
        this.website = website;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
    }

    public int getCredentialId() { return credentialId; }
    public void setCredentialId(int credentialId) { this.credentialId = credentialId; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEncryptedPassword() { return encryptedPassword; }
    public void setEncryptedPassword(String encryptedPassword) { this.encryptedPassword = encryptedPassword; }

    public String getPlainPassword() { return plainPassword; }
    public void setPlainPassword(String plainPassword) { this.plainPassword = plainPassword; }
}
