package com.musicfinder.controller;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.player.AudioPlayerManager;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.StatisticsPayload;
import com.musicfinder.shared.response.payload.ArtistData;
import com.musicfinder.shared.response.payload.SearchArtistData;
import com.musicfinder.shared.response.payload.SearchSongData;
import com.musicfinder.shared.response.payload.SongData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TrendingController {

    @FXML
    private Label trendingMessageLabel;

    @FXML
    private Label topSongTitleLabel;

    @FXML
    private Label topSongArtistLabel;

    @FXML
    private Label topArtistNameLabel;

    @FXML
    private Label topArtistDetailLabel;

    @FXML
    private Label totalSearchesLabel;

    @FXML
    private VBox topSongsContainer;

    @FXML
    private VBox topArtistsContainer;

    @FXML
    private VBox rankingsContainer;

    @FXML
    private LineChart<Number, Number> searchTrendChart;

    private final List<SongData> topSongs = new ArrayList<>();
    private final List<ArtistData> topArtists = new ArrayList<>();

    @FXML
    private void initialize() {
        trendingMessageLabel.setText("");
        loadTrendingData();
        setupChart();
    }

    private void loadTrendingData() {
        // Top songs
        try {
            Request songReq = Request.of(Action.GET_TOP_SONGS, new StatisticsPayload("SONG", 10));
            ClientConnection.getInstance().send(songReq, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        SearchSongData data = response.dataAs(SearchSongData.class);
                        topSongs.clear();
                        if (data != null && data.songs() != null) {
                            topSongs.addAll(data.songs());
                        }
                        renderTopSongs();
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi tải top bài hát: " + e.getMessage());
        }

        // Top artists
        try {
            Request artistReq = Request.of(Action.GET_TOP_ARTISTS, new StatisticsPayload("ARTIST", 10));
            ClientConnection.getInstance().send(artistReq, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        SearchArtistData data = response.dataAs(SearchArtistData.class);
                        topArtists.clear();
                        if (data != null && data.artists() != null) {
                            topArtists.addAll(data.artists());
                        }
                        renderTopArtists();
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi tải top nghệ sĩ: " + e.getMessage());
        }
    }

    private void renderTopSongs() {
        topSongsContainer.getChildren().clear();
        rankingsContainer.getChildren().clear();

        if (topSongs.isEmpty()) {
            topSongTitleLabel.setText("No data");
            topSongArtistLabel.setText("Chưa có lượt tìm kiếm");
            return;
        }

        SongData top1 = topSongs.get(0);
        topSongTitleLabel.setText(top1.title());
        String art = top1.artistNames() != null ? String.join(", ", top1.artistNames()) : "Unknown";
        topSongArtistLabel.setText(art + " • " + (top1.searchCount() != null ? top1.searchCount() : 0) + " searches");

        long total = 0;
        for (SongData s : topSongs) {
            if (s.searchCount() != null) total += s.searchCount();
        }
        totalSearchesLabel.setText(String.valueOf(total));

        for (int i = 0; i < Math.min(5, topSongs.size()); i++) {
            SongData song = topSongs.get(i);
            HBox row = createSongBarRow(song, i + 1);
            topSongsContainer.getChildren().add(row);
        }

        for (int i = 0; i < Math.min(4, topSongs.size()); i++) {
            SongData song = topSongs.get(i);
            HBox row = createRankingRow(song, i + 1);
            rankingsContainer.getChildren().add(row);
        }
    }

    private HBox createSongBarRow(SongData song, int rank) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("bar-row");

        Label rankNum = new Label(String.format("%02d", rank));
        rankNum.getStyleClass().add("rank-number");

        VBox titleBox = new VBox(1);
        Label title = new Label(song.title());
        title.getStyleClass().add("bar-name");
        String artist = song.artistNames() != null ? String.join(", ", song.artistNames()) : "Unknown";
        Label art = new Label(artist);
        art.setStyle("-fx-text-fill: #7e758a; -fx-font-size: 8px;");
        titleBox.getChildren().addAll(title, art);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label count = new Label((song.searchCount() != null ? song.searchCount() : 0) + " searches");
        count.getStyleClass().add("bar-value");

        Button playBtn = new Button("▶");
        playBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #bd85ff; -fx-cursor: hand;");
        playBtn.setOnAction(e -> AudioPlayerManager.getInstance().playSong(song, topSongs));

        row.getChildren().addAll(rankNum, titleBox, count, playBtn);
        return row;
    }

    private HBox createRankingRow(SongData song, int rank) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("ranking-row");

        Label num = new Label("#" + rank);
        num.getStyleClass().add("ranking-number");

        VBox info = new VBox(2);
        Label title = new Label(song.title());
        title.getStyleClass().add("ranking-title");
        String artist = song.artistNames() != null ? String.join(", ", song.artistNames()) : "Unknown";
        Label art = new Label(artist);
        art.getStyleClass().add("ranking-subtitle");
        info.getChildren().addAll(title, art);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button playBtn = new Button("▶");
        playBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #bd85ff; -fx-cursor: hand;");
        playBtn.setOnAction(e -> AudioPlayerManager.getInstance().playSong(song, topSongs));

        row.getChildren().addAll(num, info, playBtn);
        return row;
    }

    private void renderTopArtists() {
        topArtistsContainer.getChildren().clear();
        if (topArtists.isEmpty()) {
            topArtistNameLabel.setText("No data");
            topArtistDetailLabel.setText("Chưa có lượt tìm kiếm");
            return;
        }

        ArtistData top1 = topArtists.get(0);
        topArtistNameLabel.setText(top1.name());
        topArtistDetailLabel.setText((top1.searchCount() != null ? top1.searchCount() : 0) + " searches");

        for (int i = 0; i < Math.min(4, topArtists.size()); i++) {
            ArtistData a = topArtists.get(i);
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("bar-row");

            Label rankNum = new Label(String.format("%02d", i + 1));
            rankNum.getStyleClass().add("rank-number");

            Label name = new Label(a.name());
            name.getStyleClass().add("bar-name");
            HBox.setHgrow(name, Priority.ALWAYS);

            Label val = new Label((a.searchCount() != null ? a.searchCount() : 0) + " searches");
            val.getStyleClass().add("bar-value");

            row.getChildren().addAll(rankNum, name, val);
            topArtistsContainer.getChildren().add(row);
        }
    }

    private void setupChart() {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>(1, 15));
        series.getData().add(new XYChart.Data<>(2, 28));
        series.getData().add(new XYChart.Data<>(3, 22));
        series.getData().add(new XYChart.Data<>(4, 45));
        series.getData().add(new XYChart.Data<>(5, 38));
        series.getData().add(new XYChart.Data<>(6, 62));
        series.getData().add(new XYChart.Data<>(7, 55));
        searchTrendChart.getData().clear();
        searchTrendChart.getData().add(series);
    }

    private void showMessage(String msg) {
        trendingMessageLabel.setText(msg);
    }
}
