package com.musicfinder.controller;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.player.AudioPlayerManager;
import com.musicfinder.session.UserSession;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.enums.PlayList_Type;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.Add_RemoveToPlaylistPayload;
import com.musicfinder.shared.response.payload.GetFavoritesData;
import com.musicfinder.shared.response.payload.SongData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FavoritesController {

    @FXML
    private Label favoriteCountLabel;

    @FXML
    private Label favoriteMessageLabel;

    @FXML
    private VBox songTable;

    private final List<SongData> favoriteSongs = new ArrayList<>();

    @FXML
    private void initialize() {
        favoriteMessageLabel.setText("");
        if (!UserSession.getInstance().isLoggedIn()) {
            showMessage("Vui lòng đăng nhập để xem danh sách bài hát yêu thích.");
            return;
        }
        loadFavorites();
    }

    private void loadFavorites() {
        try {
            Request request = Request.of(Action.GET_FAVORITES, null);
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        GetFavoritesData data = response.dataAs(GetFavoritesData.class);
                        favoriteSongs.clear();
                        if (data != null && data.songs() != null) {
                            favoriteSongs.addAll(data.songs());
                        }
                        renderSongs();
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Không thể tải bài hát yêu thích");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối tới server: " + e.getMessage());
        }
    }

    private void renderSongs() {
        // Giữ lại dòng table-header (dòng đầu tiên)
        if (songTable.getChildren().size() > 1) {
            songTable.getChildren().remove(1, songTable.getChildren().size());
        }

        favoriteCountLabel.setText(favoriteSongs.size() + (favoriteSongs.size() == 1 ? " song" : " songs"));

        if (favoriteSongs.isEmpty()) {
            showMessage("Danh sách bài hát yêu thích trống. Hãy nhấn ❤️ ở trang Search để thêm bài hát!");
            return;
        }

        for (int i = 0; i < favoriteSongs.size(); i++) {
            SongData song = favoriteSongs.get(i);
            HBox row = createSongRow(song, i);
            songTable.getChildren().add(row);
        }
    }

    private HBox createSongRow(SongData song, int index) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("song-row");
        if (index == favoriteSongs.size() - 1) {
            row.getStyleClass().add("last-row");
        }

        // Rank
        Label indexLabel = new Label(String.format("%02d", index + 1));
        indexLabel.setPrefWidth(35);
        indexLabel.getStyleClass().add("row-index");

        // Cover thumbnail
        ImageView cover = new ImageView();
        cover.setFitWidth(42);
        cover.setFitHeight(42);
        cover.getStyleClass().add("song-cover");
        if (song.thumbnail_url() != null && !song.thumbnail_url().isBlank()) {
            try {
                cover.setImage(new Image(song.thumbnail_url(), true));
            } catch (Exception ignored) {
            }
        }

        // Title and artist
        VBox titleBox = new VBox(3);
        Label titleLabel = new Label(song.title() != null ? song.title() : "Unknown");
        titleLabel.getStyleClass().add("song-title");

        String artists = song.artistNames() != null && !song.artistNames().isEmpty()
                ? String.join(", ", song.artistNames())
                : "Unknown";
        Label artistLabel = new Label(artists);
        artistLabel.getStyleClass().add("song-meta");

        titleBox.getChildren().addAll(titleLabel, artistLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Album placeholder
        Label albumLabel = new Label("Single");
        albumLabel.setPrefWidth(240);
        albumLabel.getStyleClass().add("album-text");

        // Duration
        int seconds = song.durations_seconds() != null ? song.durations_seconds() : 0;
        String durationStr = String.format("%d:%02d", seconds / 60, seconds % 60);
        Label durationLabel = new Label(durationStr);
        durationLabel.setPrefWidth(90);
        durationLabel.getStyleClass().add("duration-text");

        // Actions: Play and Remove
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_LEFT);
        actionsBox.setPrefWidth(130);

        Button playBtn = new Button("▶");
        playBtn.getStyleClass().addAll("action-button");
        playBtn.setOnAction(e -> AudioPlayerManager.getInstance().playSong(song, favoriteSongs));

        Button removeBtn = new Button("💔");
        removeBtn.getStyleClass().addAll("action-button", "heart-action");
        removeBtn.setOnAction(e -> handleRemoveFavorite(song));

        actionsBox.getChildren().addAll(playBtn, removeBtn);

        row.getChildren().addAll(indexLabel, cover, titleBox, albumLabel, durationLabel, actionsBox);
        return row;
    }

    private void handleRemoveFavorite(SongData song) {
        if (!UserSession.getInstance().isLoggedIn()) return;
        try {
            Add_RemoveToPlaylistPayload payload = new Add_RemoveToPlaylistPayload(
                    null,
                    song,
                    PlayList_Type.FAVORITES
            );
            Request request = Request.of(Action.REMOVE_FAVORITE, payload);
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        favoriteSongs.remove(song);
                        renderSongs();
                        showMessage("Đã bỏ yêu thích: " + song.title());
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Xóa thất bại");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối tới server: " + e.getMessage());
        }
    }

    private void showMessage(String message) {
        favoriteMessageLabel.setText(message);
    }
}
