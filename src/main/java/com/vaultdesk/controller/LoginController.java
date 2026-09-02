package com.vaultdesk.controller;

import com.vaultdesk.service.AuthenticationService;
import com.vaultdesk.service.BackupService;
import com.vaultdesk.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.format.DateTimeFormatter;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthenticationService authService = AuthenticationService.getInstance();
    private final BackupService backupService = new BackupService();

    @FXML
    private void handleLogin() {
        errorLabel.setText("");
        String username = usernameField.getText().trim();
        char[] password = passwordField.getText().toCharArray();

        if (username.isEmpty() || password.length == 0) {
            errorLabel.setText("Please enter both username and master password.");
            return;
        }

        try {
            AuthenticationService.LoginResult result = authService.login(username, password);
            switch (result.status) {
                case SUCCESS -> SceneManager.showDashboard();
                case MFA_REQUIRED -> SceneManager.showMfaVerify();
                case ACCOUNT_LOCKED -> {
                    String until = result.lockedUntil == null ? "a few minutes"
                            : result.lockedUntil.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    errorLabel.setText("Account locked due to repeated failed attempts. Try again after " + until + ".");
                }
                case INVALID_CREDENTIALS -> errorLabel.setText("Invalid username or master password.");
            }
        } catch (Exception e) {
            errorLabel.setText("Login failed: " + e.getMessage());
        }
        // Note: we intentionally do NOT clear `password` here - if MFA_REQUIRED,
        // AuthenticationService is holding onto that exact char[] reference to
        // finish deriving the encryption key once the TOTP code is confirmed.
    }

    @FXML
    private void goToRegister() {
        SceneManager.showRegister();
    }

    @FXML
    private void handleRestoreBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a VaultDesk Backup File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("VaultDesk Backup", "*.vlt"));
        File file = chooser.showOpenDialog(usernameField.getScene().getWindow());
        if (file == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This will REPLACE the vault currently on this computer with the one in \""
                        + file.getName() + "\". This cannot be undone. Continue?");
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType.getButtonData().isDefaultButton()) {
                try {
                    backupService.restoreFrom(file);
                    errorLabel.setText("");
                    Alert done = new Alert(Alert.AlertType.INFORMATION,
                            "Vault restored. Log in with an account from that backup.");
                    done.showAndWait();
                } catch (Exception e) {
                    errorLabel.setText("Restore failed: " + e.getMessage());
                }
            }
        });
    }
}