package com.vaultdesk.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class FileVaultService {

    private static final Path VAULT_ROOT =
            Paths.get(System.getProperty("user.home"), ".vaultdesk", "documents");

    public String storeFile(int userId, File sourceFile) throws IOException {
        Path userDir = VAULT_ROOT.resolve(String.valueOf(userId));
        Files.createDirectories(userDir);

        String ext = getExtension(sourceFile.getName());
        String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path destination = userDir.resolve(storedName);

        Files.copy(sourceFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toString();
    }

    public String computeSha256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    public void deleteFile(String storedPath) {
        try {
            Files.deleteIfExists(Paths.get(storedPath));
        } catch (IOException ignored) {
        }
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot == -1 || dot == fileName.length() - 1) ? "" : fileName.substring(dot + 1);
    }
}
