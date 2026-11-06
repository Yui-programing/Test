package com.musicfinder.controller;

import com.musicfinder.MainApp;
import com.musicfinder.navigation.Navigator;
import com.musicfinder.network.ClientConnection;
import com.musicfinder.navigation.Screen;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class MainLayoutController {

    @FXML
    private StackPane contentContainer;

    @FXML
    private Label currentPageLabel;

    @FXML
    private Label serverStatusLabel;

    @FXML
    private Label avatarInitialLabel;

    @FXML
    private Label favoriteCountLabel;

    @FXML
    private Button homeButton;

    @FXML
    private Button searchButton;

    @FXML
    private Button artistsButton;

    @FXML
    private Button playlistsButton;

    @FXML
    private Button favoritesButton;

    @FXML
    private Button historyButton;

    @FXML
    private Button trendingButton;

    @FXML
    private Button profileButton;

    @FXML
    private HBox nowPlayingBar;

    @FXML
    private ImageView nowPlayingImage;

    @FXML
    private Label nowPlayingTitle;

    @FXML
    private Label nowPlayingArtist;

    @FXML
    private Button playPauseButton;

    private SearchController searchController;

    private final Map<Screen, Button> navigationButtons = new EnumMap<>(Screen.class);

    @FXML
    private void initialize() {
        navigationButtons.put(Screen.HOME, homeButton);
        navigationButtons.put(Screen.SEARCH, searchButton);
        navigationButtons.put(Screen.ARTISTS, artistsButton);
        navigationButtons.put(Screen.PLAYLISTS, playlistsButton);
        navigationButtons.put(Screen.FAVORITES, favoritesButton);
        navigationButtons.put(Screen.SEARCH_HISTORY, historyButton);
        navigationButtons.put(Screen.TRENDING, trendingButton);
        navigationButtons.put(Screen.PROFILE, profileButton);
        setServerConnected(ClientConnection.getInstance().isConnected());
    }

    public void show(Screen screen) throws IOException {
        if (screen.isLogin()) {
            throw new IllegalArgumentException("Login is not content of the main layout.");
        }

        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource(screen.getFxml()));

        Parent content = loader.load();
        Object controller = loader.getController();
        if (controller instanceof SearchController) {
            searchController = (SearchController) controller;
            searchController.setMainLayoutController(this);
        }
        contentContainer.getChildren().setAll(content);

        currentPageLabel.setText(screen.getHeaderTitle());
        setActiveNavigation(screen);
    }

    private void setActiveNavigation(Screen activeScreen) {
        navigationButtons.forEach((screen, button) -> {
            button.getStyleClass().remove("nav-active");
            if (screen == activeScreen) {
                button.getStyleClass().add("nav-active");
            }
        });
    }

    @FXML
    private void handleHome() throws IOException {
        Navigator.goTo(Screen.HOME);
    }

    @FXML
    private void handleSearch() throws IOException {
        Navigator.goTo(Screen.SEARCH);
    }

    @FXML
    private void handleArtists() throws IOException {
        Navigator.goTo(Screen.ARTISTS);
    }

    @FXML
    private void handlePlaylists() throws IOException {
        Navigator.goTo(Screen.PLAYLISTS);
    }

    @FXML
    private void handleFavorites() throws IOException {
        Navigator.goTo(Screen.FAVORITES);
    }

    @FXML
    private void handleHistory() throws IOException {
        Navigator.goTo(Screen.SEARCH_HISTORY);
    }

    @FXML
    private void handleTrending() throws IOException {
        Navigator.goTo(Screen.TRENDING);
    }

    @FXML
    private void handleProfile() throws IOException {
        Navigator.goTo(Screen.PROFILE);
    }

    @FXML
    private void handleSettings() {
        // Settings screen is not implemented yet.
    }

    @FXML
    private void handleLogout() throws IOException {
        Navigator.goTo(Screen.LOGIN);
    }

    public void setServerConnected(boolean connected) {
        serverStatusLabel.setText(connected ? "Server Connected" : "Server Disconnected");
    }

    public void setAvatarInitials(String initials) {
        avatarInitialLabel.setText(initials == null ? "" : initials);
    }

    public void setFavoriteCount(Integer count) {
        favoriteCountLabel.setText(count == null ? "" : String.valueOf(count));
    }

    public void setPlayingState(boolean playing) {

        if (playing) {
            playPauseButton.setText("⏸");
        } else {
            playPauseButton.setText("▶");
        }
    }

    public void showNowPlaying(String title, String artist, String thumbnailUrl) {

        nowPlayingTitle.setText(title);
        nowPlayingArtist.setText(artist);

        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            nowPlayingImage.setImage(
                    new Image(thumbnailUrl, true));
        }

        nowPlayingBar.setVisible(true);
        nowPlayingBar.setManaged(true);
    }

    @FXML
    private void handleNextSong() {

        if (searchController != null) {
            searchController.handleNextSong();
        }
    }

    @FXML
    private void handlePreviousSong() {

        if (searchController != null) {
            searchController.handlePreviousSong();
        }
    }

    @FXML
    private void handlePlayPause() {
        if (searchController != null) {
            searchController.handlePlayPause();
        }
    }
}
