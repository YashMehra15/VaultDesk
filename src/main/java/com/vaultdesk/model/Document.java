package com.vaultdesk.model;

public class Document {
    private int documentId;
    private int itemId;
    private String fileName;
    private String filePath;
    private String fileHash;

    public Document() {}

    public Document(int documentId, int itemId, String fileName, String filePath, String fileHash) {
        this.documentId = documentId;
        this.itemId = itemId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileHash = fileHash;
    }

    public int getDocumentId() { return documentId; }
    public void setDocumentId(int documentId) { this.documentId = documentId; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
}
