package com.vaultdesk.controller;

import com.vaultdesk.service.AuthenticationService;
import com.vaultdesk.service.MfaService;
import com.vaultdesk.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MfaVerifyController {

    @FXML private TextField codeField;
    @FXML private Label errorLabel;

    private final AuthenticationService authService = AuthenticationService.getInstance();
    private final MfaService mfaService = new MfaService();

    @FXML
    private void handleVerify() {
        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        if (code.isEmpty()) {
            errorLabel.setText("Enter the 6-digit code from your authenticator app.");
            return;
        }
        boolean ok = authService.verifyMfaAndCompleteLogin(code, mfaService);
        if (ok) {
            SceneManager.showDashboard();
        } else {
            errorLabel.setText("Invalid or expired code. Try again.");
        }
    }

    @FXML
    private void handleCancel() {
        authService.cancelPendingMfaLogin();
        SceneManager.showLogin();
    }
}
