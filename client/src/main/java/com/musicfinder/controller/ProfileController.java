package com.musicfinder.controller;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.session.UserSession;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.ChangePasswordProfilePayload;
import com.musicfinder.shared.request.payload.EditProfilePayload;
import com.musicfinder.shared.response.payload.GetFavoritesData;
import com.musicfinder.shared.response.payload.HistoryData;
import com.musicfinder.shared.response.payload.PlayListData;
import com.musicfinder.shared.response.payload.UserData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.IOException;

public class ProfileController {

    @FXML
    private Label profileMessageLabel;

    @FXML
    private Label avatarLargeText;

    @FXML
    private Label heroNameLabel;

    @FXML
    private Label heroSubtitleLabel;

    @FXML
    private Label fullNameValueLabel;

    @FXML
    private Label usernameValueLabel;

    @FXML
    private Label emailValueLabel;

    @FXML
    private Label totalSearchesValueLabel;

    @FXML
    private Label favoriteSongsValueLabel;

    @FXML
    private Label playlistsCountValueLabel;

    @FXML
    private void initialize() {
        profileMessageLabel.setText("");
        if (!UserSession.getInstance().isLoggedIn()) {
            showMessage("Vui lòng đăng nhập để xem thông tin tài khoản.");
            return;
        }

        loadProfile();
        loadStats();
    }

    private void loadProfile() {
        UserData current = UserSession.getInstance().getCurrentUser();
        if (current != null) {
            displayUserData(current);
        }

        try {
            Request request = Request.of(Action.GET_PROFILE, null);
            ClientConnection.getInstance().send(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        UserData fresh = response.dataAs(UserData.class);
                        UserSession.getInstance().setCurrentUser(fresh);
                        displayUserData(fresh);
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Lỗi kết nối khi tải hồ sơ: " + e.getMessage());
        }
    }

    private void displayUserData(UserData user) {
        String name = user.fullName() != null && !user.fullName().isBlank() ? user.fullName() : user.userName();
        heroNameLabel.setText(name);
        heroSubtitleLabel.setText("@" + user.userName());
        if (name != null && !name.isBlank()) {
            avatarLargeText.setText(name.substring(0, 1).toUpperCase());
        }

        fullNameValueLabel.setText(user.fullName() != null ? user.fullName() : "Chưa cập nhật");
        usernameValueLabel.setText(user.userName());
        emailValueLabel.setText(user.email() != null && !user.email().isBlank() ? user.email() : "Chưa cập nhật");
    }

    private void loadStats() {
        // Searches count
        try {
            ClientConnection.getInstance().send(Request.of(Action.GET_HISTORY, null), resp -> {
                Platform.runLater(() -> {
                    if (resp.isSuccess() && resp.getData() != null) {
                        HistoryData[] histories = resp.dataAs(HistoryData[].class);
                        totalSearchesValueLabel.setText(String.valueOf(histories != null ? histories.length : 0));
                    }
                });
            });
        } catch (Exception ignored) {
        }

        // Favorites count
        try {
            ClientConnection.getInstance().send(Request.of(Action.GET_FAVORITES, null), resp -> {
                Platform.runLater(() -> {
                    if (resp.isSuccess() && resp.getData() != null) {
                        GetFavoritesData favs = resp.dataAs(GetFavoritesData.class);
                        int count = favs != null && favs.songs() != null ? favs.songs().size() : 0;
                        favoriteSongsValueLabel.setText(String.valueOf(count));
                    }
                });
            });
        } catch (Exception ignored) {
        }

        // Playlists count
        try {
            ClientConnection.getInstance().send(Request.of(Action.GET_PLAYLISTS, null), resp -> {
                Platform.runLater(() -> {
                    if (resp.isSuccess() && resp.getData() != null) {
                        PlayListData[] pls = resp.dataAs(PlayListData[].class);
                        playlistsCountValueLabel.setText(String.valueOf(pls != null ? pls.length : 0));
                    }
                });
            });
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void handleEditProfile() {
        if (!UserSession.getInstance().isLoggedIn()) return;
        UserData current = UserSession.getInstance().getCurrentUser();

        Dialog<EditProfilePayload> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa thông tin cá nhân");
        dialog.setHeaderText("Cập nhật họ tên và email của bạn");

        ButtonType saveBtnType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 100, 10, 10));

        TextField fullNameField = new TextField(current != null && current.fullName() != null ? current.fullName() : "");
        TextField emailField = new TextField(current != null && current.email() != null ? current.email() : "");

        grid.add(new Label("Họ và tên:"), 0, 0);
        grid.add(fullNameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                return new EditProfilePayload(fullNameField.getText().trim(), emailField.getText().trim(), null);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(payload -> {
            try {
                Request request = Request.of(Action.UPDATE_PROFILE, payload);
                ClientConnection.getInstance().send(request, response -> {
                    Platform.runLater(() -> {
                        if (response.isSuccess() && response.getData() != null) {
                            UserData updated = response.dataAs(UserData.class);
                            UserSession.getInstance().setCurrentUser(updated);
                            displayUserData(updated);
                            showMessage("Cập nhật thông tin thành công!");
                        } else {
                            showMessage(response.getMessage() != null ? response.getMessage() : "Cập nhật thất bại");
                        }
                    });
                });
            } catch (IOException e) {
                showMessage("Lỗi kết nối: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleChangePassword() {
        if (!UserSession.getInstance().isLoggedIn()) return;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Đổi mật khẩu");
        dialog.setHeaderText("Nhập mật khẩu mới (tối thiểu 6 ký tự)");

        ButtonType changeBtnType = new ButtonType("Đổi mật khẩu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 100, 10, 10));

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("Mật khẩu mới");
        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Xác nhận mật khẩu mới");

        grid.add(new Label("Mật khẩu mới:"), 0, 0);
        grid.add(newPassField, 1, 0);
        grid.add(new Label("Nhập lại:"), 0, 1);
        grid.add(confirmPassField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == changeBtnType) {
                String p1 = newPassField.getText().trim();
                String p2 = confirmPassField.getText().trim();
                if (!p1.equals(p2)) {
                    return null;
                }
                return p1;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newPass -> {
            if (newPass.length() < 6) {
                showMessage("Mật khẩu mới phải có ít nhất 6 ký tự và 2 ô phải trùng nhau.");
                return;
            }

            try {
                ChangePasswordProfilePayload payload = new ChangePasswordProfilePayload(newPass);
                Request request = Request.of(Action.CHANGE_PASSWORD, payload);
                ClientConnection.getInstance().send(request, response -> {
                    Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            showMessage("Đổi mật khẩu thành công!");
                        } else {
                            showMessage(response.getMessage() != null ? response.getMessage() : "Đổi mật khẩu thất bại");
                        }
                    });
                });
            } catch (IOException e) {
                showMessage("Lỗi kết nối: " + e.getMessage());
            }
        });
    }

    private void showMessage(String message) {
        profileMessageLabel.setText(message);
    }
}
