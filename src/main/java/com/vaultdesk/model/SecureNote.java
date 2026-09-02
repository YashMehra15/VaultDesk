package com.vaultdesk.model;

public class SecureNote {
    private int noteId;
    private int itemId;
    private String encryptedContent;
    private transient String plainContent;

    public SecureNote() {}

    public SecureNote(int noteId, int itemId, String encryptedContent) {
        this.noteId = noteId;
        this.itemId = itemId;
        this.encryptedContent = encryptedContent;
    }

    public int getNoteId() { return noteId; }
    public void setNoteId(int noteId) { this.noteId = noteId; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getEncryptedContent() { return encryptedContent; }
    public void setEncryptedContent(String encryptedContent) { this.encryptedContent = encryptedContent; }

    public String getPlainContent() { return plainContent; }
    public void setPlainContent(String plainContent) { this.plainContent = plainContent; }
}
