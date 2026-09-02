package com.vaultdesk.controller;

import com.vaultdesk.dao.CredentialDAO;
import com.vaultdesk.dao.DocumentDAO;
import com.vaultdesk.dao.NoteDAO;
import com.vaultdesk.dao.SecurityLogDAO;
import com.vaultdesk.dao.VaultItemDAO;
import com.vaultdesk.model.Credential;
import com.vaultdesk.model.User;
import com.vaultdesk.model.VaultItem;
import com.vaultdesk.service.BackupService;
import com.vaultdesk.service.EncryptionService;
import com.vaultdesk.service.SecurityScoreService;
import com.vaultdesk.service.SessionManager;
import com.vaultdesk.util.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DashboardController {

    private static final int AUTO_LOCK_SECONDS = 5 * 60;

    @FXML private Label welcomeLabel;
    @FXML private Label lockCountdownLabel;
    @FXML private Label passwordCountLabel;
    @FXML private Label noteCountLabel;
    @FXML private Label documentCountLabel;
    @FXML private Label securityScoreLabel;
    @FXML private ProgressBar securityScoreBar;
    @FXML private Label strongLabel;
    @FXML private Label reusedLabel;
    @FXML private Label oldLabel;
    @FXML private Label weakLabel;
    @FXML private Label backupStatusLabel;
    @FXML private javafx.scene.control.TabPane mainTabPane;
    @FXML private javafx.scene.control.Tab overviewTab;

    private final VaultItemDAO vaultItemDAO = new VaultItemDAO();
    private final CredentialDAO credentialDAO = new CredentialDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final SecurityScoreService scoreService = new SecurityScoreService();
    private final SecurityLogDAO securityLogDAO = new SecurityLogDAO();
    private final BackupService backupService = new BackupService();

    private int secondsRemaining = AUTO_LOCK_SECONDS;
    private Timeline autoLockTimeline;

    @FXML
    private void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            SceneManager.showLogin();
            return;
        }
        welcomeLabel.setText("Welcome back, " + user.getUsername());

        refreshCounts();
        refreshSecurityScore();
        startAutoLockTimer();

        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == overviewTab) {
                    refreshCounts();
                    refreshSecurityScore();
                }
            });
        }

        Platform.runLater(() -> {
            if (welcomeLabel.getScene() != null) {
                welcomeLabel.getScene().addEventFilter(javafx.scene.input.MouseEvent.ANY, e -> resetAutoLockTimer());
                welcomeLabel.getScene().addEventFilter(javafx.scene.input.KeyEvent.ANY, e -> resetAutoLockTimer());
            }
        });
    }

    private void refreshCounts() {
        User user = SessionManager.getInstance().getCurrentUser();
        int passwords = vaultItemDAO.findByUserAndType(user.getUserId(), VaultItem.ItemType.CREDENTIAL).size();
        int notes = vaultItemDAO.findByUserAndType(user.getUserId(), VaultItem.ItemType.NOTE).size();
        int documents = documentDAO.findAllByUser(user.getUserId()).size();

        passwordCountLabel.setText(String.valueOf(passwords));
        noteCountLabel.setText(String.valueOf(notes));
        documentCountLabel.setText(String.valueOf(documents));
    }

    @FXML
    private void refreshSecurityScore() {
        User user = SessionManager.getInstance().getCurrentUser();
        EncryptionService encryption = SessionManager.getInstance().getEncryptionService();

        List<Credential> credentials = credentialDAO.findAllByUser(user.getUserId());
        Map<Integer, VaultItem> items = vaultItemDAO.findAllByUserAsMap(user.getUserId());

        for (Credential c : credentials) {
            try {
                c.setPlainPassword(encryption.decrypt(c.getEncryptedPassword()));
            } catch (Exception ignored) {
            }
        }

        SecurityScoreService.Report report = scoreService.analyze(items, credentials);

        securityScoreLabel.setText(report.score + "%");
        securityScoreBar.setProgress(report.score / 100.0);
        strongLabel.setText("Strong Passwords: " + report.strongCount);
        reusedLabel.setText("Reused Passwords: " + report.reusedCount);
        oldLabel.setText("Old Passwords: " + report.oldCount);
        weakLabel.setText("Weak Passwords: " + report.weakCount);

        credentials.forEach(c -> c.setPlainPassword(null));
    }

    @FXML
    private void handleBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Vault Backup");
        chooser.setInitialFileName("vaultdesk_backup.vlt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("VaultDesk Backup", "*.vlt"));
        File file = chooser.showSaveDialog(welcomeLabel.getScene().getWindow());
        if (file == null) return;

        try {
            backupService.backupTo(file);
            User user = SessionManager.getInstance().getCurrentUser();
            securityLogDAO.record(user.getUserId(), "Vault backup created");
            backupStatusLabel.setText("✓ Backup saved to " + file.getName());
        } catch (Exception e) {
            backupStatusLabel.setText("Backup failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleRestore() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a Backup to Restore");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("VaultDesk Backup", "*.vlt"));
        File file = chooser.showOpenDialog(welcomeLabel.getScene().getWindow());
        if (file == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This replaces your ENTIRE current vault with the backup file and logs you out. Continue?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || !result.get().getButtonData().isDefaultButton()) return;

        try {
            backupService.restoreFrom(file);
            SessionManager.getInstance().lock();
            SceneManager.showLogin();
        } catch (Exception e) {
            backupStatusLabel.setText("Restore failed: " + e.getMessage());
        }
    }

    private void startAutoLockTimer() {
        secondsRemaining = AUTO_LOCK_SECONDS;
        autoLockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickAutoLock()));
        autoLockTimeline.setCycleCount(Timeline.INDEFINITE);
        autoLockTimeline.play();
    }

    private void resetAutoLockTimer() {
        secondsRemaining = AUTO_LOCK_SECONDS;
    }

    private void tickAutoLock() {
        secondsRemaining--;
        int minutes = Math.max(secondsRemaining, 0) / 60;
        int seconds = Math.max(secondsRemaining, 0) % 60;
        lockCountdownLabel.setText(String.format("Auto-lock in %d:%02d", minutes, seconds));

        if (secondsRemaining <= 0) {
            lockVault();
        }
    }

    private void lockVault() {
        if (autoLockTimeline != null) autoLockTimeline.stop();
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            securityLogDAO.record(user.getUserId(),
                    secondsRemaining <= 0 ? "Vault auto-locked (inactivity)" : "Vault locked (logout)");
        }
        SessionManager.getInstance().lock();
        SceneManager.showLogin();
    }

    @FXML
    private void handleLogout() {
        lockVault();
    }
}
