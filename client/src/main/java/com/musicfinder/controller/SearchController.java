package com.musicfinder.controller;

import com.musicfinder.navigation.Navigator;
import com.musicfinder.navigation.Screen;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.SearchPayload;
import com.musicfinder.shared.response.payload.SearchSongData;
import com.musicfinder.shared.response.payload.SongData;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchController {

    @FXML
    private TextField searchField;

    @FXML
    private Label searchMessageLabel;

    @FXML
    private VBox searchResultsList;

    @FXML
    private Label resultTitleLabel;

    @FXML
    private Label resultSubtitleLabel;

    // play song
    private MediaPlayer mediaPlayer;

    private SongData currentSong;

    private boolean isPlaying = false;

    private MainLayoutController mainLayoutController;

    private List<SongData> currentSongList = new ArrayList<>();

    private int currentSongIndex = -1;

    public void setMainLayoutController(MainLayoutController mainLayoutController) {

        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    private void initialize() {
        searchMessageLabel.setText("");
    }

    @FXML
    private void handleSearch() throws IOException {

        String keyword = searchField.getText() == null
                ? ""
                : searchField.getText().trim();

        if (keyword.isEmpty()) {
            searchField.requestFocus();
            showMessage("Enter a song title to search.");
            return;
        }

        SearchPayload payload = new SearchPayload(keyword);

        Request request = Request.of(
                Action.SEARCH_SONG,
                payload);

        ClientConnection.getInstance().send(request, response -> {
            if (!response.isSuccess()) {
                Platform.runLater(() -> {
                    showMessage(response.getMessage());
                });
                return;
            }
            SearchSongData data = response.dataAs(SearchSongData.class);
            System.out.println("Số bài hát: " + data.songs().size());
            currentSongList.clear();
            currentSongList.addAll(data.songs());
            Platform.runLater(() -> {
                showMessage("Found " + currentSongList.size() + " songs");
                renderSongs(currentSongList);
            });
        });
    }

    @FXML
    private void handleNavPlaceholder() {
        showMessage("This screen will be connected when we build it in the next steps.");
    }

    private void showMessage(String message) {
        searchMessageLabel.setText(message);
    }

    private void renderSongs(List<SongData> songs) {
        searchResultsList.getChildren().clear();
        if (songs == null || songs.isEmpty()) {
            resultSubtitleLabel.setText("No Songs Found");
            return;
        }
        resultSubtitleLabel.setText(songs.size() + " songs found");
        for (int i = 0; i < songs.size(); i++) {
            SongData song = songs.get(i);
            HBox row = createSongRow(song, i);
            searchResultsList.getChildren().add(row);
        }
    }

    private HBox createSongRow(SongData song, int index) {

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("popular-button");

        // ===== RANK =====

        Label rankLabel = new Label(String.format("%02d", index + 1));

        rankLabel.getStyleClass().add("rank");

        // ===== THUMBNAIL =====

        ImageView thumbnail = new ImageView();

        thumbnail.setFitWidth(52);
        thumbnail.setFitHeight(52);
        thumbnail.setPreserveRatio(false);

        thumbnail.getStyleClass().add("cover-thumb");

        if (song.thumbnail_url() != null && !song.thumbnail_url().isBlank()) {

            try {
                Image image = new Image(song.thumbnail_url(), true);
                thumbnail.setImage(image);
            } catch (Exception e) {
                System.out.println(
                        "Cannot load thumbnail: "
                                + song.thumbnail_url());
            }
        }

        // ===== TITLE =====

        Label titleLabel = new Label(song.title());

        titleLabel.getStyleClass().add("song-title");

        // ===== ARTIST =====

        String artists = song.artistNames() == null ? ""
                : String.join(", ", song.artistNames());

        Label artistLabel = new Label(artists);

        artistLabel.getStyleClass().add("song-artist");

        // ===== SONG INFO =====

        VBox songInfo = new VBox(5);

        songInfo.setAlignment(Pos.CENTER_LEFT);

        songInfo.getChildren().addAll(titleLabel, artistLabel);

        HBox.setHgrow(songInfo, Priority.ALWAYS);

        songInfo.setMinWidth(0);

        // ===== SEARCH COUNT =====

        long searchCount = song.searchCount() == null ? 0 : song.searchCount();

        Label countLabel = new Label(searchCount + " searches");

        countLabel.getStyleClass().add("search-count");

        SVGPath heartIcon = new SVGPath();
        heartIcon.setContent(
                "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 " +
                        "2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09 " +
                        "C13.09 3.81 14.76 3 16.5 3 " +
                        "19.58 3 22 5.42 22 8.5 " +
                        "c0 3.78-3.4 6.86-8.55 11.54L12 21.35z");

        heartIcon.getStyleClass().add("favorite-icon");

        Button favoriteButton = new Button();
        favoriteButton.setGraphic(heartIcon);

        favoriteButton.getStyleClass().addAll("song-action-btn", "favorite-btn");

        Button playListButton = new Button("🎶");
        playListButton.getStyleClass().addAll("song-action-btn", "playList-btn");

        // ===== PLAY =====

        Button playButton = new Button("▶");

        playButton.getStyleClass().addAll("song-action-btn", "row-play");

        HBox actionGroup = new HBox(8, countLabel, favoriteButton, playListButton, playButton);
        actionGroup.setAlignment(Pos.CENTER_RIGHT);
        actionGroup.getStyleClass().add("song-actions");
        actionGroup.setMinWidth(Region.USE_PREF_SIZE);

        row.getChildren().addAll(rankLabel, thumbnail, songInfo, actionGroup);

        // event
        playButton.setOnAction(event -> {
            currentSongIndex = index;
            handlePlaySong(song);
            event.consume();
        });
        playListButton.setOnAction(event -> {
            handleAddToPlaylist(song);
        });
        favoriteButton.setOnAction(event -> {
            handlleAddToFavorite(song);
        });

        return row;
    }

    private void handlePlaySong(SongData song) {

        System.out.println(
                "Clicked song: "
                        + song.songID()
                        + " - "
                        + song.title()
                        + " action: Play");

        currentSong = song;

        String artists = song.artistNames() == null
                ? "Unknown artist"
                : String.join(", ", song.artistNames());

        /*
         * SearchController không tự đụng UI main-layout.
         * Gửi dữ liệu sang MainLayoutController.
         */
        if (mainLayoutController != null) {

            mainLayoutController.showNowPlaying(
                    song.title(),
                    artists,
                    song.thumbnail_url());
        }

        playAudio(song);
    }

    private void handleAddToPlaylist(SongData song) {

        System.out.println(
                "Clicked song: "
                        + song.songID()
                        + " - "
                        + song.title() + " action: playlist");
    }

    private void handlleAddToFavorite(SongData song) {

        System.out.println(
                "Clicked song: "
                        + song.songID()
                        + " - "
                        + song.title() + "action: favorite");
    }

    private void playAudio(
            SongData song) {

        String audioUrl = song.audio_url();

        // Nếu audio_url không có
        // thử preview_url

        if (audioUrl == null
                || audioUrl.isBlank()) {

            audioUrl = song.preview_url();
        }

        if (audioUrl == null
                || audioUrl.isBlank()) {

            System.out.println(
                    "Song không có audio URL");

            return;
        }

        try {

            // =========================
            // DỪNG BÀI CŨ
            // =========================

            if (mediaPlayer != null) {

                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            // =========================
            // TẠO MEDIA
            // =========================

            Media media = new Media(audioUrl);

            mediaPlayer = new MediaPlayer(media);

            // =========================
            // KHI LOAD XONG
            // =========================

            mediaPlayer.setOnReady(() -> {

                System.out.println(
                        "Audio ready: "
                                + song.title());

                mediaPlayer.play();

                isPlaying = true;
                if (mainLayoutController != null) {
                    mainLayoutController.setPlayingState(true);
                }
            });

            // =========================
            // KHI PHÁT HẾT
            // =========================

            mediaPlayer.setOnEndOfMedia(() -> {

                isPlaying = false;
                if (mainLayoutController != null) {
                    mainLayoutController.setPlayingState(true);
                }
            });

            // =========================
            // ERROR
            // =========================

            mediaPlayer.setOnError(() -> {

                System.out.println(
                        "MediaPlayer error: "
                                + mediaPlayer
                                        .getError());
            });

        } catch (Exception e) {

            System.out.println(
                    "Cannot play audio: "
                            + e.getMessage());
        }
    }

    @FXML
    public void handlePlayPause() {

        if (mediaPlayer == null) {
            return;
        }

        if (isPlaying) {

            mediaPlayer.pause();

            isPlaying = false;
            if (mainLayoutController != null) {
                mainLayoutController.setPlayingState(true);
            }

        } else {

            mediaPlayer.play();

            isPlaying = true;
            if (mainLayoutController != null) {
                mainLayoutController.setPlayingState(true);
            }
        }
    }

    public void handleNextSong() {

        if (currentSongList == null || currentSongList.isEmpty()) {
            return;
        }

        if (currentSongIndex == -1) {
            return;
        }

        currentSongIndex++;

        if (currentSongIndex >= currentSongList.size()) {
            currentSongIndex = 0;
        }

        SongData nextSong = currentSongList.get(currentSongIndex);

        handlePlaySong(nextSong);
    }

    public void handlePreviousSong() {

        if (currentSongList == null || currentSongList.isEmpty()) {
            return;
        }

        if (currentSongIndex == -1) {
            return;
        }

        currentSongIndex--;

        if (currentSongIndex < 0) {
            currentSongIndex = currentSongList.size() - 1;
        }

        SongData previousSong = currentSongList.get(currentSongIndex);

        handlePlaySong(previousSong);
    }
}
