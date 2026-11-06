package com.musicfinder.controller;

import com.musicfinder.navigation.Navigator;
import com.musicfinder.navigation.Screen;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.Add_RemoveToPlaylistPayload;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class FavoritesController {

    @FXML
    private HBox favoritesRoot;

    @FXML
    private Label favoriteMessageLabel;

    @FXML
    private void initialize() throws IOException {
        favoriteMessageLabel.setText("");
    }

    @FXML
    private void handleAddToFavorite(Long songID) throws IOException {
        // Add_RemoveToPlaylistPayload payload = new Add_RemoveToPlaylistPayload();
        Request request = Request.of(Action.ADD_FAVORITE, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleRemoveToFavorite(Long songID) throws IOException {
        // Add_RemoveToPlaylistPayload payload = new Add_RemoveToPlaylistPayload();
        Request request = Request.of(Action.REMOVE_FAVORITE, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleNavPlaceholder() {
        showMessage("This screen will be connected when we build it in the next steps.");
    }

    private void showMessage(String message) {
        favoriteMessageLabel.setText(message);
    }
}
