package com.vaultdesk.controller;

import com.vaultdesk.model.User;
import com.vaultdesk.service.AuthenticationService;
import com.vaultdesk.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Arrays;
import java.util.Optional;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private final AuthenticationService authService = AuthenticationService.getInstance();

    @FXML
    private void handleRegister() {
        errorLabel.setText("");
        String username = usernameField.getText().trim();
        char[] password = passwordField.getText().toCharArray();
        char[] confirm = confirmPasswordField.getText().toCharArray();

        if (username.isEmpty() || password.length == 0) {
            errorLabel.setText("Username and master password are required.");
            return;
        }
        if (password.length < 8) {
            errorLabel.setText("Master password must be at least 8 characters.");
            return;
        }
        if (!Arrays.equals(password, confirm)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        try {
            Optional<User> created = authService.register(username, password);
            if (created.isPresent()) {
                SceneManager.showLogin();
            } else {
                errorLabel.setText("That username is already taken.");
            }
        } catch (Exception e) {
            errorLabel.setText("Registration failed: " + e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    @FXML
    private void goToLogin() {
        SceneManager.showLogin();
    }
}
