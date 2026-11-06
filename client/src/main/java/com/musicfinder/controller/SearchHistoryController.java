package com.musicfinder.controller;

import com.musicfinder.navigation.Navigator;
import com.musicfinder.navigation.Screen;
import com.musicfinder.network.ClientConnection;
import com.musicfinder.session.UserSession;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.RemoveRowSearchHistory;
import com.musicfinder.shared.response.payload.HistoryData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryController {

    @FXML
    private VBox historyTable;

    @FXML
    private Label historyMessageLabel;

    @FXML
    private Button allTabButton;

    @FXML
    private Button songsTabButton;

    @FXML
    private Button artistsTabButton;

    private final List<HistoryData> allHistory = new ArrayList<>();
    private String currentFilter = "ALL";

    @FXML
    private void initialize() {
        historyMessageLabel.setText("");
        if (!UserSession.getInstance().isLoggedIn()) {
            showMessage("Vui lòng đăng nhập để xem lịch sử tìm kiếm.");
            return;
        }
        loadHistory();
    }

    private void loadHistory() {
        try {
            Request request = Request.of(Action.GET_HISTORY, null);
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        HistoryData[] data = response.dataAs(HistoryData[].class);
                        allHistory.clear();
                        if (data != null) {
                            allHistory.addAll(List.of(data));
                        }
                        renderHistory();
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Không thể tải lịch sử tìm kiếm");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối tới server: " + e.getMessage());
        }
    }

    private void renderHistory() {
        // Giữ lại dòng table-header
        if (historyTable.getChildren().size() > 1) {
            historyTable.getChildren().remove(1, historyTable.getChildren().size());
        }

        List<HistoryData> filtered = allHistory.stream()
                .filter(h -> {
                    if ("SONG".equals(currentFilter)) return "SONG".equalsIgnoreCase(h.searchType());
                    if ("ARTIST".equals(currentFilter)) return "ARTIST".equalsIgnoreCase(h.searchType());
                    return true;
                })
                .toList();

        if (filtered.isEmpty()) {
            showMessage("Không có lịch sử tìm kiếm nào.");
            return;
        }

        for (int i = 0; i < filtered.size(); i++) {
            HistoryData h = filtered.get(i);
            HBox row = createHistoryRow(h, i == filtered.size() - 1);
            historyTable.getChildren().add(row);
        }
    }

    private HBox createHistoryRow(HistoryData h, boolean isLast) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        if (isLast) {
            row.getStyleClass().add("last-row");
        }
        row.setPadding(new javafx.geometry.Insets(12, 16, 12, 16));

        // Keyword
        Label keywordLabel = new Label(h.keyword());
        keywordLabel.setPrefWidth(350);
        keywordLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        keywordLabel.setOnMouseClicked(e -> {
            try {
                if ("ARTIST".equalsIgnoreCase(h.searchType())) {
                    Navigator.goTo(Screen.ARTISTS);
                } else {
                    Navigator.goTo(Screen.SEARCH);
                }
            } catch (Exception ignored) {
            }
        });

        // Type
        Label typeLabel = new Label(h.searchType() != null ? h.searchType() : "SONG");
        typeLabel.setPrefWidth(130);
        typeLabel.setStyle("-fx-text-fill: #aa7bff; -fx-font-size: 11px;");

        // Result count
        int count = h.resultCount() != null ? h.resultCount() : 0;
        Label resultLabel = new Label(count + " kết quả");
        resultLabel.setPrefWidth(150);
        resultLabel.setStyle("-fx-text-fill: #8e8499; -fx-font-size: 11px;");

        // Date
        String dateStr = h.searchedAt() != null ? h.searchedAt().toString() : "";
        Label dateLabel = new Label(dateStr);
        dateLabel.setPrefWidth(180);
        dateLabel.setStyle("-fx-text-fill: #8e8499; -fx-font-size: 11px;");

        // Delete Action
        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff6e85; -fx-cursor: hand;");
        delBtn.setOnAction(e -> handleRemoveRow(h));

        HBox actionBox = new HBox(delBtn);
        HBox.setHgrow(actionBox, Priority.ALWAYS);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(keywordLabel, typeLabel, resultLabel, dateLabel, actionBox);
        return row;
    }

    @FXML
    private void handleClearHistory() {
        if (!UserSession.getInstance().isLoggedIn()) return;
        try {
            Request request = Request.of(Action.CLEAR_HISTORY, null);
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        allHistory.clear();
                        renderHistory();
                        showMessage("Đã xóa toàn bộ lịch sử tìm kiếm");
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Xóa lịch sử thất bại");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối server: " + e.getMessage());
        }
    }

    private void handleRemoveRow(HistoryData h) {
        if (!UserSession.getInstance().isLoggedIn() || h.historyId() == null) return;
        try {
            RemoveRowSearchHistory payload = new RemoveRowSearchHistory(h.historyId());
            Request request = Request.of(Action.REMOVE_ROW_HISTORY, payload);
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        allHistory.remove(h);
                        renderHistory();
                        showMessage("Đã xóa mục lịch sử");
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Xóa mục thất bại");
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowAll() {
        currentFilter = "ALL";
        setActiveFilter(allTabButton);
        renderHistory();
    }

    @FXML
    private void handleShowSongs() {
        currentFilter = "SONG";
        setActiveFilter(songsTabButton);
        renderHistory();
    }

    @FXML
    private void handleShowArtists() {
        currentFilter = "ARTIST";
        setActiveFilter(artistsTabButton);
        renderHistory();
    }

    private void setActiveFilter(Button activeButton) {
        allTabButton.getStyleClass().remove("filter-tab-active");
        songsTabButton.getStyleClass().remove("filter-tab-active");
        artistsTabButton.getStyleClass().remove("filter-tab-active");

        if (!activeButton.getStyleClass().contains("filter-tab-active")) {
            activeButton.getStyleClass().add("filter-tab-active");
        }
    }

    private void showMessage(String message) {
        historyMessageLabel.setText(message);
    }
}
