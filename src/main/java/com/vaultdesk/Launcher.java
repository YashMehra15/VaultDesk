package com.vaultdesk;

/**
 * Entry point used ONLY by the packaged standalone jar / native installer.
 * Routing through a plain, non-Application main() avoids the "JavaFX
 * runtime components are missing" error some JVMs throw when a JavaFX
 * Application class is launched directly from an executable jar.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
