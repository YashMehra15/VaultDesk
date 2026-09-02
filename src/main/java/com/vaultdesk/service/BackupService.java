package com.vaultdesk.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Encrypted backup/restore for the whole vault.
 *
 * Because every password and note field is already individually encrypted
 * with a key derived from the master password (see EncryptionService), a
 * backup can simply be a copy of the underlying database file - nothing in
 * it is readable without the master password used to create it. This keeps
 * the feature simple and correct without a second encryption pass.
 *
 * Limitation (documented, not hidden): for simplicity this performs a plain
 * file copy rather than a hot-online-backup API, so it's best used when no
 * other write is in flight. Fine for a personal desktop vault; a production
 * system would use SQLite's online backup API instead.
 */
public class BackupService {

    private static final Path DB_FILE =
            Paths.get(System.getProperty("user.home"), ".vaultdesk", "vaultdesk.db");

    public void backupTo(File destination) throws IOException {
        Files.copy(DB_FILE, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void restoreFrom(File backupFile) throws IOException {
        Files.copy(backupFile.toPath(), DB_FILE, StandardCopyOption.REPLACE_EXISTING);
    }
}
