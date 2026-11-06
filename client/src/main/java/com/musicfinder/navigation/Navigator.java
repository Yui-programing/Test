package com.musicfinder.navigation;

import com.musicfinder.MainApp;
import com.musicfinder.controller.MainLayoutController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class Navigator {

    private static final double DEFAULT_WIDTH = 1200;
    private static final double DEFAULT_HEIGHT = 760;

    private static Stage stage;
    private static Parent mainLayoutRoot;
    private static MainLayoutController mainLayoutController;

    private Navigator() {
    }

    public static void initialize(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void goTo(Screen screen) throws IOException {
        ensureInitialized();

        if (screen.isLogin()) {
            showLogin(screen);
            return;
        }

        ensureMainLayout();
        mainLayoutController.show(screen);
        applyWindowSettings(screen);
    }

    private static void showLogin(Screen screen) throws IOException {
        Parent root = FXMLLoader.load(
                MainApp.class.getResource(screen.getFxml()));

        setSceneRoot(root);
        mainLayoutRoot = null;
        mainLayoutController = null;
        applyWindowSettings(screen);
    }

    private static void ensureMainLayout() throws IOException {
        if (mainLayoutRoot != null && mainLayoutController != null) {
            if (stage.getScene() == null || stage.getScene().getRoot() != mainLayoutRoot) {
                setSceneRoot(mainLayoutRoot);
            }
            return;
        }

        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/musicfinder/main-layout.fxml"));

        mainLayoutRoot = loader.load();
        mainLayoutController = loader.getController();
        setSceneRoot(mainLayoutRoot);
    }

    private static void setSceneRoot(Parent root) {
        Scene scene = stage.getScene();

        if (scene == null) {
            stage.setScene(new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT));
        } else {
            scene.setRoot(root);
        }
    }

    private static void applyWindowSettings(Screen screen) {
        stage.setTitle(screen.getTitle());
        stage.setMinWidth(screen.getMinWidth());
        stage.setMinHeight(screen.getMinHeight());
    }

    private static void ensureInitialized() {
        if (stage == null) {
            throw new IllegalStateException("Navigator has not been initialized.");
        }
    }
}
