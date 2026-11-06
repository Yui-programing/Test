package com.musicfinder.controller;

import com.musicfinder.navigation.Navigator;
import com.musicfinder.navigation.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import java.io.IOException;

public class HomeController {

    @FXML
    private TextField searchField;

    @FXML
    private Label homeMessageLabel;

    @FXML
    private void initialize() {
        homeMessageLabel.setText("");

        // Cho ảnh bìa trong các card co giãn theo bề rộng card (giống Figma)
        searchField.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene == null)
                return;
            scene.getRoot().lookupAll(".card-image").forEach(node -> {
                if (node instanceof ImageView img && img.getParent() instanceof Region card) {
                    img.fitWidthProperty().bind(card.widthProperty().subtract(16));
                }
            });
        });
    }

    @FXML
    private void handleSearch() throws IOException {
        Navigator.goTo(Screen.SEARCH);
    }

    @FXML
    private void handleExplore() throws IOException {
        Navigator.goTo(Screen.SEARCH);
    }

    @FXML
    private void handleGoToSearch() throws IOException {
        Navigator.goTo(Screen.SEARCH);
    }

    @FXML
    private void handleGoToArtists() throws IOException {
        Navigator.goTo(Screen.ARTISTS);
    }

    @FXML
    private void handleGoSearchHistory() throws IOException {
        Navigator.goTo(Screen.SEARCH_HISTORY);
    }

    @FXML
    private void handleNavPlaceholder() {
        homeMessageLabel.setText("This feature will be connected later.");
    }
}
