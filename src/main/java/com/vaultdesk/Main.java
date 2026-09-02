package com.vaultdesk;

import com.vaultdesk.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("VaultDesk - Personal Digital Vault");
        primaryStage.setMinWidth(950);
        primaryStage.setMinHeight(680);
        SceneManager.init(primaryStage);
        SceneManager.showLogin();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
