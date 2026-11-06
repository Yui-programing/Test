package com.musicfinder.controller;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.player.AudioPlayerManager;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.ArtistPayload;
import com.musicfinder.shared.request.payload.SearchPayload;
import com.musicfinder.shared.request.payload.StatisticsPayload;
import com.musicfinder.shared.response.payload.ArtistData;
import com.musicfinder.shared.response.payload.SearchArtistData;
import com.musicfinder.shared.response.payload.SongData;
import com.musicfinder.shared.response.payload.ViewArtistDetailData;
import com.musicfinder.shared.response.payload.ViewArtistSongsData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArtistsController {

    @FXML
    private TextField artistSearchField;

    @FXML
    private Label artistMessageLabel;

    @FXML
    private VBox artistsContainer;

    private final List<ArtistData> currentArtists = new ArrayList<>();

    @FXML
    private void initialize() {
        artistMessageLabel.setText("");
        loadFeaturedArtists();
    }

    private void loadFeaturedArtists() {
        try {
            Request request = Request.of(Action.GET_TOP_ARTISTS, new StatisticsPayload("ARTIST", 10));
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        SearchArtistData data = response.dataAs(SearchArtistData.class);
                        currentArtists.clear();
                        if (data != null && data.artists() != null) {
                            currentArtists.addAll(data.artists());
                        }
                        renderArtists();
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối server: " + e.getMessage());
        }
    }

    @FXML
    private void handleArtistSearch() {
        String keyword = artistSearchField.getText() == null ? "" : artistSearchField.getText().trim();
        if (keyword.isEmpty()) {
            artistSearchField.requestFocus();
            showMessage("Vui lòng nhập tên nghệ sĩ để tìm kiếm.");
            return;
        }

        try {
            SearchPayload payload = new SearchPayload(keyword);
            Request request = Request.of(Action.SEARCH_ARTIST, payload);
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        SearchArtistData data = response.dataAs(SearchArtistData.class);
                        currentArtists.clear();
                        if (data != null && data.artists() != null) {
                            currentArtists.addAll(data.artists());
                        }
                        showMessage("Tìm thấy " + currentArtists.size() + " nghệ sĩ");
                        renderArtists();
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Không tìm thấy nghệ sĩ");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối tới server: " + e.getMessage());
        }
    }

    private void renderArtists() {
        artistsContainer.getChildren().clear();
        if (currentArtists.isEmpty()) {
            Label empty = new Label("Không có thông tin nghệ sĩ.");
            empty.setStyle("-fx-text-fill: #8e8499; -fx-padding: 20;");
            artistsContainer.getChildren().add(empty);
            return;
        }

        for (ArtistData artist : currentArtists) {
            HBox card = createArtistCard(artist);
            artistsContainer.getChildren().add(card);
        }
    }

    private HBox createArtistCard(ArtistData artist) {
        HBox card = new HBox(18);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("artist-card");
        card.setPadding(new Insets(16, 20, 16, 20));

        // Photo
        ImageView photo = new ImageView();
        photo.setFitWidth(68);
        photo.setFitHeight(68);
        photo.getStyleClass().add("artist-photo");
        if (artist.image_url() != null && !artist.image_url().isBlank()) {
            try {
                photo.setImage(new Image(artist.image_url(), true));
            } catch (Exception ignored) {
            }
        }

        // Info
        VBox info = new VBox(4);
        Label eyebrow = new Label("VIETNAMESE ARTIST");
        eyebrow.getStyleClass().add("card-eyebrow");

        Label name = new Label(artist.name());
        name.getStyleClass().add("artist-name");

        String location = artist.homeTown() != null && !artist.homeTown().isBlank()
                ? "Quê quán: " + artist.homeTown()
                : "Việt Nam";
        Label locLabel = new Label(location);
        locLabel.getStyleClass().add("artist-location");

        String bio = artist.biagraphy() != null && !artist.biagraphy().isBlank()
                ? artist.biagraphy()
                : "Chưa có thông tin tiểu sử chi tiết.";
        if (bio.length() > 160) {
            bio = bio.substring(0, 160) + "...";
        }
        Label bioLabel = new Label(bio);
        bioLabel.getStyleClass().add("artist-description");
        bioLabel.setWrapText(true);

        info.getChildren().addAll(eyebrow, name, locLabel, bioLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Actions
        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button songsBtn = new Button("Xem bài hát 🎵");
        songsBtn.getStyleClass().addAll("secondary-button", "small-button");
        songsBtn.setOnAction(e -> handleViewSong(artist.artistID(), artist.name()));

        Button detailBtn = new Button("Chi tiết ℹ");
        detailBtn.getStyleClass().addAll("primary-button", "small-button");
        detailBtn.setOnAction(e -> handleViewDetail(artist.artistID()));

        actions.getChildren().addAll(songsBtn, detailBtn);

        card.getChildren().addAll(photo, info, actions);
        return card;
    }

    private void handleViewDetail(Long artistID) {
        if (artistID == null) return;
        try {
            Request request = Request.of(Action.VIEW_ARTIST_DETAIL, new ArtistPayload(artistID));
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        ViewArtistDetailData detail = response.dataAs(ViewArtistDetailData.class);
                        showArtistDetailDialog(detail != null ? detail.artist() : null);
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Không thể tải chi tiết nghệ sĩ");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối: " + e.getMessage());
        }
    }

    private void showArtistDetailDialog(ArtistData artist) {
        if (artist == null) return;
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Thông tin ca sĩ: " + artist.name());
        dialog.setHeaderText(artist.name());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(480);

        if (artist.birth_date() != null) {
            content.getChildren().add(new Label("Ngày sinh: " + artist.birth_date()));
        }
        if (artist.homeTown() != null && !artist.homeTown().isBlank()) {
            content.getChildren().add(new Label("Quê quán: " + artist.homeTown()));
        }

        Label bioTitle = new Label("Tiểu sử:");
        bioTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        Label bioText = new Label(artist.biagraphy() != null && !artist.biagraphy().isBlank() ? artist.biagraphy() : "Chưa có thông tin");
        bioText.setWrapText(true);
        bioText.setStyle("-fx-text-fill: #d8cbea;");

        ScrollPane sp = new ScrollPane(bioText);
        sp.setPrefHeight(200);
        sp.setFitToWidth(true);

        content.getChildren().addAll(bioTitle, sp);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void handleViewSong(Long artistID, String artistName) {
        if (artistID == null) return;
        try {
            Request request = Request.of(Action.VIEW_ARTIST_SONGS, new ArtistPayload(artistID));
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        ViewArtistSongsData data = response.dataAs(ViewArtistSongsData.class);
                        showArtistSongsDialog(artistName, data != null && data.songs() != null ? data.songs() : List.of());
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Không có bài hát nào");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối: " + e.getMessage());
        }
    }

    private void showArtistSongsDialog(String artistName, List<SongData> songs) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Bài hát của: " + artistName);
        dialog.setHeaderText("Danh sách bài hát (" + songs.size() + ")");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox list = new VBox(8);
        list.setPadding(new Insets(10));
        list.setPrefWidth(500);

        if (songs.isEmpty()) {
            list.getChildren().add(new Label("Chưa có bài hát nào của nghệ sĩ này trong hệ thống."));
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
                info.getChildren().add(title);
                HBox.setHgrow(info, Priority.ALWAYS);

                Button playBtn = new Button("▶");
                playBtn.setStyle("-fx-background-color: #bd85ff; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
                playBtn.setOnAction(e -> AudioPlayerManager.getInstance().playSong(song, songs));

                row.getChildren().addAll(rank, info, playBtn);
                list.getChildren().add(row);
            }
        }

        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setPrefHeight(350);
        dialog.getDialogPane().setContent(sp);
        dialog.showAndWait();
    }

    private void showMessage(String message) {
        artistMessageLabel.setText(message);
    }
}
