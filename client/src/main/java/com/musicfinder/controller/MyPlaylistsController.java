package com.musicfinder.controller;

import com.musicfinder.navigation.Navigator;
import com.musicfinder.navigation.Screen;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.Add_RemoveToPlaylistPayload;
import com.musicfinder.shared.request.payload.Create_UpdateToPlaylistPayload;
import com.musicfinder.shared.request.payload.Remove_OpenPlaylistPayload;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class MyPlaylistsController {

    @FXML
    private HBox playlistRoot;

    @FXML
    private Label playlistMessageLabel;

    @FXML
    private void initialize() throws IOException {

        playlistMessageLabel.setText("");
    }

    @FXML
    private void handleCreatePlaylist() throws IOException {
        // Create_UpdateToPlaylistPayload payload = new
        // Create_UpdateToPlaylistPayload();
        Request request = Request.of(Action.CREATE_PLAYLIST, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleEditPlaylist() throws IOException {
        // Create_UpdateToPlaylistPayload payload = new
        // Create_UpdateToPlaylistPayload();
        Request request = Request.of(Action.EDIT_FROM_PLAYLIST, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleDeletePlaylist() throws IOException {
        // Remove_OpenPlaylistPayload payload = new Remove_OpenPlaylistPayload();
        Request request = Request.of(Action.DELETE_PLAYLIST, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleOpenPlaylist() throws IOException {
        // Remove_OpenPlaylistPayload payload = new Remove_OpenPlaylistPayload();
        Request request = Request.of(Action.GET_PLAYLIST_SONGS, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleAddSongToPlaylist() throws IOException {
        // Add_RemoveToPlaylistPayload payload = new Add_RemoveToPlaylistPayload();
        Request request = Request.of(Action.ADD_TO_PLAYLIST, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleRemoveSongFromPlaylist() throws IOException {
        // Add_RemoveToPlaylistPayload payload = new Add_RemoveToPlaylistPayload();
        Request request = Request.of(Action.REMOVE_FROM_PLAYLIST, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void GetAll() throws IOException {
        Request request = Request.of(Action.GET_PLAYLISTS, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleGoHome() throws IOException {
        Navigator.goTo(Screen.HOME);
    }

    @FXML
    private void handleGoSearch() throws IOException {
        Navigator.goTo(Screen.SEARCH);
    }

    @FXML
    private void handleGoArtists() throws IOException {
        Navigator.goTo(Screen.ARTISTS);
    }

    @FXML
    private void handleGoFavorites() throws IOException {
        Navigator.goTo(Screen.FAVORITES);
    }

    @FXML
    private void handleGoSearchHistory() throws IOException {
        Navigator.goTo(Screen.SEARCH_HISTORY);
    }

    @FXML
    private void handleGoTrending() throws IOException {
        Navigator.goTo(Screen.TRENDING);
    }

    @FXML
    private void handleGoProfile() throws IOException {
        Navigator.goTo(Screen.PROFILE);
    }

    @FXML
    private void handleNavPlaceholder() {
        showMessage("This screen will be connected when we build it in the next steps.");
    }

    private void showMessage(String message) {
        playlistMessageLabel.setText(message);
    }
}
