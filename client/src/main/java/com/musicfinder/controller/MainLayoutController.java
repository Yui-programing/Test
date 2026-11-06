package com.musicfinder.controller;

import com.musicfinder.MainApp;
import com.musicfinder.navigation.Navigator;
import com.musicfinder.network.ClientConnection;
import com.musicfinder.navigation.Screen;
import com.musicfinder.player.AudioPlayerManager;
import com.musicfinder.session.UserSession;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.response.payload.GetFavoritesData;
import com.musicfinder.shared.response.payload.UserData;
import javafx.application.Platform;
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

        // Cập nhật thông tin avatar khi có UserSession
        UserSession.getInstance().addListener(this::updateUserInfo);
        updateUserInfo(UserSession.getInstance().getCurrentUser());

        // Lắng nghe trình phát nhạc
        AudioPlayerManager player = AudioPlayerManager.getInstance();
        player.addSongChangeListener(song -> {
            if (song != null) {
                String artist = song.artistNames() != null ? String.join(", ", song.artistNames()) : "Unknown";
                showNowPlaying(song.title(), artist, song.thumbnail_url());
            }
        });
        player.addPlayStateListener(this::setPlayingState);
    }

    private void updateUserInfo(UserData user) {
        if (user != null) {
            String name = user.fullName() != null && !user.fullName().isBlank()
                    ? user.fullName()
                    : user.userName();
            if (name != null && !name.isBlank()) {
                setAvatarInitials(name.substring(0, 1).toUpperCase());
            }
            refreshFavoritesCount();
        } else {
            setAvatarInitials("");
            setFavoriteCount(0);
        }
    }

    public void refreshFavoritesCount() {
        if (!UserSession.getInstance().isLoggedIn()) return;
        try {
            Request req = Request.of(Action.GET_FAVORITES, null);
            ClientConnection.getInstance().send(req, resp -> {
                if (resp.isSuccess() && resp.getData() != null) {
                    GetFavoritesData favs = resp.dataAs(GetFavoritesData.class);
                    Platform.runLater(() -> {
                        int count = favs != null && favs.songs() != null ? favs.songs().size() : 0;
                        setFavoriteCount(count);
                    });
                }
            });
        } catch (Exception ignored) {
        }
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
        // Settings screen
    }

    @FXML
    private void handleLogout() throws IOException {
        AudioPlayerManager.getInstance().stop();
        try {
            ClientConnection.getInstance().send(Request.of(Action.LOGOUT, null));
        } catch (Exception ignored) {
        }
        UserSession.getInstance().clear();
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
            try {
                nowPlayingImage.setImage(new Image(thumbnailUrl, true));
            } catch (Exception ignored) {
            }
        }

        nowPlayingBar.setVisible(true);
        nowPlayingBar.setManaged(true);
    }

    @FXML
    private void handleNextSong() {
        AudioPlayerManager.getInstance().next();
    }

    @FXML
    private void handlePreviousSong() {
        AudioPlayerManager.getInstance().previous();
    }

    @FXML
    private void handlePlayPause() {
        AudioPlayerManager.getInstance().togglePlayPause();
    }
}
