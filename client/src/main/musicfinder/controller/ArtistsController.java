package com.musicfinder.controller;

import com.musicfinder.navigation.Navigator;
import com.musicfinder.navigation.Screen;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.ArtistPayload;
import com.musicfinder.shared.request.payload.SearchPayload;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class ArtistsController {

    @FXML
    private TextField artistSearchField;

    @FXML
    private Label artistMessageLabel;

    @FXML
    private void initialize() {
        artistMessageLabel.setText("");
    }

    @FXML
    private void handleArtistSearch() throws IOException {

        String keyword = artistSearchField.getText() == null
                ? ""
                : artistSearchField.getText().trim();

        if (keyword.isEmpty()) {
            showMessage("Enter an artist name to search.");
            return;
        }

        SearchPayload payload = new SearchPayload(keyword);

        Request request = Request.of(
                Action.SEARCH_ARTIST,
                payload);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
            System.out.println(response.getMessage());
        });
    }

    @FXML
    private void handleViewDetail(Long artistID) throws IOException {
        // ArtistPayload payload = new ArtistPayload(artistID);
        Request request = Request.of(Action.VIEW_ARTIST_DETAIL, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
            System.out.println(response.getMessage());
        });
    }

    @FXML
    private void handleViewSong(Long artistID) throws IOException {
        // ArtistPayload payload = new ArtistPayload(artistID);
        Request request = Request.of(Action.VIEW_ARTIST_SONGS, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
            System.out.println(response.getMessage());
        });
    }

    @FXML
    private void handleNavPlaceholder() {
        showMessage("This screen will be connected when we build it in the next steps.");
    }

    private void showMessage(String message) {
        artistMessageLabel.setText(message);
    }
}
