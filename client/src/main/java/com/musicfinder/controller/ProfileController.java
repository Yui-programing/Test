package com.musicfinder.controller;

import com.musicfinder.navigation.Navigator;
import com.musicfinder.navigation.Screen;

import com.musicfinder.network.ClientConnection;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.ChangePasswordProfilePayload;
import com.musicfinder.shared.request.payload.EditProfilePayload;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class ProfileController {

    @FXML
    private HBox profileRoot;

    @FXML
    private Label profileMessageLabel;

    @FXML
    private void initialize() throws IOException {

        profileMessageLabel.setText("");
    }

    @FXML
    private void getInfo() throws IOException {
        Request request = Request.of(Action.GET_PROFILE, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleEditProfile() throws IOException {
        // EditProfilePayload payload = new EditProfilePayload();
        Request request = Request.of(Action.UPDATE_PROFILE, null);
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleChangePassword() throws IOException {
        // ChangePasswordProfilePayload payload = new ChangePasswordProfilePayload();
        Request request = Request.of(Action.UPDATE_PROFILE, null); //đã có bổ xung thêm actions là CHANGE_PASSWORD 
        //có thể thay đổi
        ClientConnection.getInstance().send(request, response -> {
            // UI UPDATE LATER
        });
    }

    @FXML
    private void handleSettingsPlaceholder() {
        showMessage("Settings will be built as a separate screen in the next step.");
    }

    private void showMessage(String message) {
        profileMessageLabel.setText(message);
    }
}
