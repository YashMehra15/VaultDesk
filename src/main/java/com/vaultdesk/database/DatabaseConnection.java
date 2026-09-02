package com.vaultdesk.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Central JDBC connection provider - embedded SQLite, zero setup required.
 *
 * Lives at %USERPROFILE%\.vaultdesk\vaultdesk.db (or the home-folder
 * equivalent on macOS/Linux). Schema is created and migrated automatically
 * on first use, so there's nothing to install or configure.
 */
public final class DatabaseConnection {

    private static final Path DB_DIR = Paths.get(System.getProperty("user.home"), ".vaultdesk");
    private static final Path DB_FILE = DB_DIR.resolve("vaultdesk.db");
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static volatile boolean initialized = false;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            Files.createDirectories(DB_DIR);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database configuration", e);
        }
    }

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement pragma = conn.createStatement()) {
            pragma.execute("PRAGMA foreign_keys = ON");
        }
        ensureSchema(conn);
        return conn;
    }

    private static void ensureSchema(Connection conn) throws SQLException {
        if (initialized) return;
        synchronized (DatabaseConnection.class) {
            if (initialized) return;
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                        username      TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
                        failed_attempts INTEGER DEFAULT 0,
                        locked_until    DATETIME NULL,
                        mfa_secret      TEXT NULL,
                        mfa_enabled     INTEGER DEFAULT 0
                    )
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS vault_items (
                        item_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id     INTEGER NOT NULL,
                        title       TEXT NOT NULL,
                        category    TEXT DEFAULT 'Other',
                        item_type   TEXT NOT NULL CHECK (item_type IN ('CREDENTIAL','NOTE','DOCUMENT')),
                        created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                        is_deleted  INTEGER DEFAULT 0,
                        deleted_at  DATETIME NULL,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                    )
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS credentials (
                        credential_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                        item_id            INTEGER NOT NULL,
                        website            TEXT,
                        username           TEXT,
                        encrypted_password TEXT NOT NULL,
                        FOREIGN KEY (item_id) REFERENCES vault_items(item_id) ON DELETE CASCADE
                    )
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS secure_notes (
                        note_id           INTEGER PRIMARY KEY AUTOINCREMENT,
                        item_id           INTEGER NOT NULL,
                        encrypted_content TEXT NOT NULL,
                        FOREIGN KEY (item_id) REFERENCES vault_items(item_id) ON DELETE CASCADE
                    )
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS documents (
                        document_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                        item_id      INTEGER NOT NULL,
                        file_name    TEXT NOT NULL,
                        file_path    TEXT NOT NULL,
                        file_hash    TEXT NOT NULL,
                        FOREIGN KEY (item_id) REFERENCES vault_items(item_id) ON DELETE CASCADE
                    )
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS security_logs (
                        log_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id    INTEGER NOT NULL,
                        action     TEXT NOT NULL,
                        timestamp  DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                    )
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tags (
                        tag_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        name    TEXT NOT NULL,
                        UNIQUE(user_id, name),
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                    )
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS item_tags (
                        item_id INTEGER NOT NULL,
                        tag_id  INTEGER NOT NULL,
                        PRIMARY KEY (item_id, tag_id),
                        FOREIGN KEY (item_id) REFERENCES vault_items(item_id) ON DELETE CASCADE,
                        FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
                    )
                    """);

                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vault_items_user ON vault_items(user_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_credentials_item ON credentials(item_id)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notes_item ON secure_notes(item_id)");

                // --- Migration safety net: if an older vaultdesk.db (pre-2.0) is being
                // reopened, these columns/tables won't exist yet from CREATE TABLE IF NOT
                // EXISTS alone (that only applies to brand-new tables). Add them if missing,
                // ignoring "duplicate column" errors on databases that already have them.
                addColumnIfMissing(st, "users", "failed_attempts", "INTEGER DEFAULT 0");
                addColumnIfMissing(st, "users", "locked_until", "DATETIME NULL");
                addColumnIfMissing(st, "users", "mfa_secret", "TEXT NULL");
                addColumnIfMissing(st, "users", "mfa_enabled", "INTEGER DEFAULT 0");
                addColumnIfMissing(st, "vault_items", "is_deleted", "INTEGER DEFAULT 0");
                addColumnIfMissing(st, "vault_items", "deleted_at", "DATETIME NULL");
            }
            initialized = true;
        }
    }

    private static void addColumnIfMissing(Statement st, String table, String column, String definition) {
        try {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException ignored) {
            // Column already exists - nothing to do.
        }
    }
}
