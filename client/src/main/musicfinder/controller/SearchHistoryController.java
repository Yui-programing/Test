package com.musicfinder.controller;

import com.musicfinder.navigation.Navigator;
import com.musicfinder.navigation.Screen;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.RemoveRowSearchHistory;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class SearchHistoryController {

    @FXML
    private HBox historyRoot;
    @FXML
    private VBox historyTable;
    @FXML
    private Label historyMessageLabel;
    @FXML
    private HBox songRowOne;
    @FXML
    private HBox artistRowOne;
    @FXML
    private HBox songRowTwo;
    @FXML
    private HBox artistRowTwo;
    @FXML
    private Button allTabButton;
    @FXML
    private Button songsTabButton;
    @FXML
    private Button artistsTabButton;

    @FXML
    private void initialize() throws IOException {

        historyMessageLabel.setText("");
    }

    @FXML
    private void getAll() throws IOException {
        Request request = Request.of(Action.GET_HISTORY, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleClearHistory() throws IOException {
        Request request = Request.of(Action.CLEAR_HISTORY, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleRemoveRow() throws IOException {
        // RemoveRowSearchHistory payload = new RemoveRowSearchHistory();
        Request request = Request.of(Action.REMOVE_ROW_HISTORY, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleShowAll() {
        setActiveFilter(allTabButton);
    }

    @FXML
    private void handleShowSongs() {
        setActiveFilter(songsTabButton);
    }

    @FXML
    private void handleShowArtists() {
        setActiveFilter(artistsTabButton);
    }

    private void setActiveFilter(Button activeButton) {
        allTabButton.getStyleClass().remove("filter-tab-active");
        songsTabButton.getStyleClass().remove("filter-tab-active");
        artistsTabButton.getStyleClass().remove("filter-tab-active");

        if (!activeButton.getStyleClass().contains("filter-tab-active")) {
            activeButton.getStyleClass().add("filter-tab-active");
        }
    }

    @FXML
    private void handleNavPlaceholder() {
        historyMessageLabel.setText("This screen will be connected when we build it in the next steps.");
    }
}
