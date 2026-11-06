package com.musicfinder.controller;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.player.AudioPlayerManager;
import com.musicfinder.session.UserSession;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.enums.PlayList_Type;
import com.musicfinder.shared.request.payload.Add_RemoveToPlaylistPayload;
import com.musicfinder.shared.request.payload.Create_UpdateToPlaylistPayload;
import com.musicfinder.shared.request.payload.Remove_OpenPlaylistPayload;
import com.musicfinder.shared.response.payload.GetPlaylistSongsData;
import com.musicfinder.shared.response.payload.PlayListData;
import com.musicfinder.shared.response.payload.SongData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyPlaylistsController {

    @FXML
    private Label playlistMessageLabel;

    @FXML
    private Label collectionsCountLabel;

    @FXML
    private FlowPane playlistsContainer;

    private final List<PlayListData> playlists = new ArrayList<>();

    @FXML
    private void initialize() {
        playlistMessageLabel.setText("");
        if (!UserSession.getInstance().isLoggedIn()) {
            showMessage("Vui lòng đăng nhập để quản lý playlist.");
            return;
        }
        loadPlaylists();
    }

    private void loadPlaylists() {
        try {
            Request request = Request.of(Action.GET_PLAYLISTS, null);
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        PlayListData[] data = response.dataAs(PlayListData[].class);
                        playlists.clear();
                        if (data != null) {
                            playlists.addAll(List.of(data));
                        }
                        renderPlaylists();
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Không thể tải danh sách playlist");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối server: " + e.getMessage());
        }
    }

    private void renderPlaylists() {
        playlistsContainer.getChildren().clear();
        collectionsCountLabel.setText(playlists.size() + (playlists.size() == 1 ? " collection" : " collections"));

        if (playlists.isEmpty()) {
            showMessage("Bạn chưa có playlist nào. Hãy tạo playlist mới bằng nút bên trên!");
            return;
        }

        for (PlayListData pl : playlists) {
            VBox card = createPlaylistCard(pl);
            playlistsContainer.getChildren().add(card);
        }
    }

    private VBox createPlaylistCard(PlayListData pl) {
        VBox card = new VBox(0);
        card.getStyleClass().add("playlist-card");
        card.setPrefWidth(240);
        card.setMinWidth(240);

        // Header cover box
        StackPane coverBox = new StackPane();
        coverBox.setPrefHeight(130);
        coverBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #4a2d75, #26173d); -fx-background-radius: 10 10 0 0;");
        Label iconLabel = new Label("🎵");
        iconLabel.setStyle("-fx-font-size: 32px; -fx-opacity: 0.8;");
        coverBox.getChildren().add(iconLabel);

        // Body
        VBox body = new VBox(6);
        body.getStyleClass().add("playlist-card-body");
        body.setPadding(new Insets(14));

        Label eyebrow = new Label("PLAYLIST");
        eyebrow.getStyleClass().add("card-eyebrow");

        Label nameLabel = new Label(pl.name());
        nameLabel.getStyleClass().add("playlist-name");

        String desc = pl.description() != null && !pl.description().isBlank() ? pl.description() : "Không có mô tả";
        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("playlist-description");
        descLabel.setWrapText(true);

        Separator sep = new Separator();
        sep.getStyleClass().add("card-separator");

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button openBtn = new Button("Mở playlist →");
        openBtn.getStyleClass().add("open-link");
        openBtn.setOnAction(e -> handleOpenPlaylist(pl));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("icon-button");
        deleteBtn.setOnAction(e -> handleDeletePlaylist(pl));

        actionRow.getChildren().addAll(openBtn, spacer, deleteBtn);
        body.getChildren().addAll(eyebrow, nameLabel, descLabel, sep, actionRow);

        card.getChildren().addAll(coverBox, body);
        return card;
    }

    @FXML
    private void handleCreatePlaylist() {
        if (!UserSession.getInstance().isLoggedIn()) {
            showMessage("Vui lòng đăng nhập để tạo playlist.");
            return;
        }

        Dialog<Create_UpdateToPlaylistPayload> dialog = new Dialog<>();
        dialog.setTitle("Tạo Playlist Mới");
        dialog.setHeaderText("Nhập thông tin playlist của bạn");

        ButtonType createBtnType = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 100, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Tên playlist");
        TextField descField = new TextField();
        descField.setPromptText("Mô tả ngắn (tùy chọn)");

        grid.add(new Label("Tên playlist:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Mô tả:"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == createBtnType) {
                return new Create_UpdateToPlaylistPayload(null, nameField.getText().trim(), descField.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(payload -> {
            if (payload.name().isBlank()) {
                showMessage("Tên playlist không được để trống");
                return;
            }
            try {
                Request request = Request.of(Action.CREATE_PLAYLIST, payload);
                ClientConnection.getInstance().send(request, response -> {
                    Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            showMessage("Tạo playlist thành công: " + payload.name());
                            loadPlaylists();
                        } else {
                            showMessage(response.getMessage() != null ? response.getMessage() : "Tạo playlist thất bại");
                        }
                    });
                });
            } catch (IOException e) {
                showMessage("Lỗi kết nối: " + e.getMessage());
            }
        });
    }

    private void handleDeletePlaylist(PlayListData pl) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa playlist: " + pl.name());
        confirm.setContentText("Bạn có chắc chắn muốn xóa playlist này?");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try {
                    Remove_OpenPlaylistPayload payload = new Remove_OpenPlaylistPayload(pl.playlistID());
                    Request req = Request.of(Action.DELETE_PLAYLIST, payload);
                    ClientConnection.getInstance().send(req, response -> {
                        Platform.runLater(() -> {
                            if (response.isSuccess()) {
                                showMessage("Đã xóa playlist: " + pl.name());
                                loadPlaylists();
                            } else {
                                showMessage(response.getMessage() != null ? response.getMessage() : "Xóa thất bại");
                            }
                        });
                    });
                } catch (IOException e) {
                    showMessage("Lỗi kết nối: " + e.getMessage());
                }
            }
        });
    }

    private void handleOpenPlaylist(PlayListData pl) {
        try {
            Remove_OpenPlaylistPayload payload = new Remove_OpenPlaylistPayload(pl.playlistID());
            Request req = Request.of(Action.GET_PLAYLIST_SONGS, payload);
            ClientConnection.getInstance().send(req, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        GetPlaylistSongsData data = response.dataAs(GetPlaylistSongsData.class);
                        showPlaylistSongsDialog(pl, data != null && data.songs() != null ? data.songs() : List.of());
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Không thể tải bài hát trong playlist");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối: " + e.getMessage());
        }
    }

    private void showPlaylistSongsDialog(PlayListData pl, List<SongData> songs) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Playlist: " + pl.name());
        dialog.setHeaderText(pl.name() + " (" + songs.size() + " bài hát)");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox list = new VBox(8);
        list.setPadding(new Insets(10));
        list.setPrefWidth(550);

        if (songs.isEmpty()) {
            list.getChildren().add(new Label("Playlist này chưa có bài hát nào."));
        } else {
            for (int i = 0; i < songs.size(); i++) {
                SongData song = songs.get(i);
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 8; -fx-background-color: #211d2b; -fx-background-radius: 6;");

                Label rank = new Label(String.format("%02d", i + 1));
                rank.setStyle("-fx-text-fill: #aa7bff; -fx-font-weight: bold;");

                VBox info = new VBox(2);
                Label title = new Label(song.title());
                title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                String artist = song.artistNames() != null ? String.join(", ", song.artistNames()) : "Unknown";
                Label art = new Label(artist);
                art.setStyle("-fx-text-fill: #8e8499; -fx-font-size: 11px;");
                info.getChildren().addAll(title, art);
                HBox.setHgrow(info, Priority.ALWAYS);

                Button playBtn = new Button("▶");
                playBtn.setStyle("-fx-background-color: #bd85ff; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
                playBtn.setOnAction(e -> AudioPlayerManager.getInstance().playSong(song, songs));

                Button removeBtn = new Button("❌");
                removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff6e85; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> {
                    handleRemoveSongFromPlaylist(pl, song, dialog);
                });

                row.getChildren().addAll(rank, info, playBtn, removeBtn);
                list.getChildren().add(row);
            }
        }

        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setPrefHeight(350);
        dialog.getDialogPane().setContent(sp);
        dialog.showAndWait();
    }

    private void handleRemoveSongFromPlaylist(PlayListData pl, SongData song, Dialog<Void> dialog) {
        try {
            Add_RemoveToPlaylistPayload payload = new Add_RemoveToPlaylistPayload(
                    pl.playlistID(),
                    song,
                    PlayList_Type.NORMAL
            );
            Request req = Request.of(Action.REMOVE_FROM_PLAYLIST, payload);
            ClientConnection.getInstance().send(req, resp -> {
                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        showMessage("Đã xóa bài hát khỏi playlist");
                        dialog.close();
                        handleOpenPlaylist(pl); // Mở lại để refresh
                    } else {
                        showMessage(resp.getMessage() != null ? resp.getMessage() : "Xóa bài hát thất bại");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối: " + e.getMessage());
        }
    }

    private void showMessage(String msg) {
        playlistMessageLabel.setText(msg);
    }
}
