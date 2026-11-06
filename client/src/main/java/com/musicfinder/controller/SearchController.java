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

import com.musicfinder.player.AudioPlayerManager;
import com.musicfinder.session.UserSession;
import com.musicfinder.shared.enums.PlayList_Type;
import com.musicfinder.shared.request.payload.Add_RemoveToPlaylistPayload;
import com.musicfinder.shared.response.payload.PlayListData;
import javafx.scene.control.ChoiceDialog;
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

    private SongData currentSong;
    private MainLayoutController mainLayoutController;
    private List<SongData> currentSongList = new ArrayList<>();

    private int currentSongIndex = -1;

    public void setMainLayoutController(MainLayoutController mainLayoutController) {

        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    private void initialize() {
        searchMessageLabel.setText("");
        AudioPlayerManager.getInstance().addStatusMessageListener(msg -> {
            Platform.runLater(() -> showMessage(msg));
        });
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
        currentSong = song;
        AudioPlayerManager.getInstance().playSong(song, currentSongList);
    }

    private void handleAddToPlaylist(SongData song) {
        if (!UserSession.getInstance().isLoggedIn()) {
            showMessage("Vui lòng đăng nhập để thêm bài hát vào danh sách phát.");
            return;
        }

        try {
            Request getPlaylistsReq = Request.of(Action.GET_PLAYLISTS, null);
            ClientConnection.getInstance().send(getPlaylistsReq, resp -> {
                Platform.runLater(() -> {
                    if (!resp.isSuccess() || resp.getData() == null) {
                        showMessage("Không thể tải danh sách playlist: " + resp.getMessage());
                        return;
                    }
                    PlayListData[] playlists = resp.dataAs(PlayListData[].class);
                    if (playlists == null || playlists.length == 0) {
                        showMessage("Bạn chưa có playlist nào. Hãy tạo playlist ở mục My Playlists.");
                        return;
                    }

                    List<PlayListData> choices = List.of(playlists);
                    ChoiceDialog<PlayListData> dialog = new ChoiceDialog<>(choices.get(0), choices);
                    dialog.setTitle("Thêm vào Playlist");
                    dialog.setHeaderText("Chọn playlist muốn thêm bài hát: " + song.title());
                    dialog.setContentText("Playlist:");

                    dialog.showAndWait().ifPresent(chosen -> {
                        try {
                            Add_RemoveToPlaylistPayload payload = new Add_RemoveToPlaylistPayload(
                                    chosen.playlistID(),
                                    song,
                                    PlayList_Type.NORMAL
                            );
                            Request addReq = Request.of(Action.ADD_TO_PLAYLIST, payload);
                            ClientConnection.getInstance().send(addReq, addResp -> {
                                Platform.runLater(() -> {
                                    if (addResp.isSuccess()) {
                                        showMessage("Đã thêm bài hát vào playlist '" + chosen.name() + "'!");
                                    } else {
                                        showMessage(addResp.getMessage() != null ? addResp.getMessage() : "Thêm thất bại");
                                    }
                                });
                            });
                        } catch (Exception ex) {
                            showMessage("Lỗi gửi yêu cầu thêm vào playlist: " + ex.getMessage());
                        }
                    });
                });
            });
        } catch (Exception e) {
            showMessage("Lỗi kết nối tới server: " + e.getMessage());
        }
    }

    private void handlleAddToFavorite(SongData song) {
        if (!UserSession.getInstance().isLoggedIn()) {
            showMessage("Vui lòng đăng nhập để thêm bài hát yêu thích.");
            return;
        }

        try {
            Add_RemoveToPlaylistPayload payload = new Add_RemoveToPlaylistPayload(
                    null,
                    song,
                    PlayList_Type.FAVORITES
            );
            Request req = Request.of(Action.ADD_FAVORITE, payload);
            ClientConnection.getInstance().send(req, resp -> {
                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        showMessage("Đã thêm vào danh sách yêu thích: " + song.title());
                        if (mainLayoutController != null) {
                            mainLayoutController.refreshFavoritesCount();
                        }
                    } else {
                        showMessage(resp.getMessage() != null ? resp.getMessage() : "Thêm yêu thích thất bại");
                    }
                });
            });
        } catch (Exception e) {
            showMessage("Lỗi kết nối: " + e.getMessage());
        }
    }

    @FXML
    public void handlePlayPause() {
        AudioPlayerManager.getInstance().togglePlayPause();
    }

    public void handleNextSong() {
        AudioPlayerManager.getInstance().next();
    }

    public void handlePreviousSong() {
        AudioPlayerManager.getInstance().previous();
    }
}
