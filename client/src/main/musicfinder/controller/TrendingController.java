package com.musicfinder.controller;

import com.musicfinder.navigation.Navigator;
import com.musicfinder.navigation.Screen;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class TrendingController {

    @FXML
    private HBox trendingRoot;

    @FXML
    private LineChart<Number, Number> searchTrendChart;

    @FXML
    private Label trendingMessageLabel;

    @FXML
    private void initialize() {

        trendingMessageLabel.setText("");
    }

    @FXML
    private void getTopArtists() throws IOException {
        Request request = Request.of(Action.GET_TOP_ARTISTS, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void getTopSongs() throws IOException {
        Request request = Request.of(Action.GET_TOP_SONGS, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleNavPlaceholder() {
        trendingMessageLabel.setText("This screen will be connected when we build it in the next step.");
    }
}
