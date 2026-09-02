package com.vaultdesk.controller;

import com.vaultdesk.dao.SecurityLogDAO;
import com.vaultdesk.dao.UserDAO;
import com.vaultdesk.model.User;
import com.vaultdesk.service.MfaService;
import com.vaultdesk.service.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class MfaSetupController {

    @FXML private Label statusLabel;
    @FXML private VBox disabledPane;
    @FXML private VBox setupPane;
    @FXML private VBox enabledPane;
    @FXML private Label secretLabel;
    @FXML private TextField confirmCodeField;
    @FXML private TextField disableCodeField;
    @FXML private Label messageLabel;
    @FXML private Label disableMessageLabel;

    private final UserDAO userDAO = new UserDAO();
    private final SecurityLogDAO securityLogDAO = new SecurityLogDAO();
    private final MfaService mfaService = new MfaService();

    private String pendingSecret;

    @FXML
    private void initialize() {
        refreshView();
    }

    private void refreshView() {
        User user = SessionManager.getInstance().getCurrentUser();
        boolean enabled = user != null && user.isMfaEnabled();

        disabledPane.setVisible(!enabled);
        disabledPane.setManaged(!enabled);
        setupPane.setVisible(false);
        setupPane.setManaged(false);
        enabledPane.setVisible(enabled);
        enabledPane.setManaged(enabled);

        statusLabel.setText(enabled ? "✓ MFA is currently ENABLED" : "MFA is currently DISABLED");
    }

    @FXML
    private void handleStartSetup() {
        pendingSecret = mfaService.generateSecret();
        secretLabel.setText(pendingSecret);

        disabledPane.setVisible(false);
        disabledPane.setManaged(false);
        setupPane.setVisible(true);
        setupPane.setManaged(true);
        messageLabel.setText("");
    }

    @FXML
    private void handleConfirmEnable() {
        String code = confirmCodeField.getText() == null ? "" : confirmCodeField.getText().trim();
        if (!mfaService.verifyCode(pendingSecret, code)) {
            messageLabel.setText("Code didn't match. Check your authenticator app and try again.");
            return;
        }
        User user = SessionManager.getInstance().getCurrentUser();
        userDAO.updateMfaSecret(user.getUserId(), pendingSecret);
        userDAO.setMfaEnabled(user.getUserId(), true);
        user.setMfaSecret(pendingSecret);
        user.setMfaEnabled(true);
        securityLogDAO.record(user.getUserId(), "MFA enabled");

        confirmCodeField.clear();
        pendingSecret = null;
        refreshView();
    }

    @FXML
    private void handleCancelSetup() {
        pendingSecret = null;
        confirmCodeField.clear();
        refreshView();
    }

    @FXML
    private void handleDisable() {
        User user = SessionManager.getInstance().getCurrentUser();
        String code = disableCodeField.getText() == null ? "" : disableCodeField.getText().trim();
        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            disableMessageLabel.setText("Enter your current 6-digit code to confirm disabling MFA.");
            return;
        }
        userDAO.setMfaEnabled(user.getUserId(), false);
        userDAO.updateMfaSecret(user.getUserId(), null);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        securityLogDAO.record(user.getUserId(), "MFA disabled");

        disableCodeField.clear();
        disableMessageLabel.setText("");
        refreshView();
    }
}
