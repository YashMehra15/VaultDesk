package com.vaultdesk.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public final class SceneManager {

    private static Stage primaryStage;

    private SceneManager() {}

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static void showLogin() {
        show("/fxml/login.fxml");
    }

    public static void showRegister() {
        show("/fxml/register.fxml");
    }

    public static void showDashboard() {
        show("/fxml/dashboard.fxml");
    }

    public static void showMfaVerify() {
        show("/fxml/mfa_verify.fxml");
    }

    private static void show(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + fxmlPath, e);
        }
    }
}
