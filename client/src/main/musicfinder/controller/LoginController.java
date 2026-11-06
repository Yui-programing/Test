package com.musicfinder.controller;

import com.musicfinder.navigation.Navigator;
import com.musicfinder.navigation.Screen;
import com.musicfinder.network.ClientConnection;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.LoginPayload;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberCheckBox;

    @FXML
    private Label messageLabel;

    @FXML
    private TextField passwordVisibleField;

    @FXML
    private HBox usernameBox;

    @FXML
    private HBox passwordBox;

    @FXML
    private Button togglePasswordButton;

    @FXML
    private Node eyeIcon;

    @FXML
    private Node eyeOffIcon;

    private static final PseudoClass FOCUSED_WITHIN = PseudoClass.getPseudoClass("focused-within");

    @FXML
    private void initialize() {
        // Hai ô mật khẩu (ẩn/hiện) dùng chung nội dung
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        // Viền tím sáng cho cả khung khi ô bên trong được focus
        usernameField.focusedProperty()
                .addListener((obs, was, now) -> usernameBox.pseudoClassStateChanged(FOCUSED_WITHIN, now));
        passwordField.focusedProperty()
                .addListener((obs, was, now) -> passwordBox.pseudoClassStateChanged(FOCUSED_WITHIN, now));
        passwordVisibleField.focusedProperty()
                .addListener((obs, was, now) -> passwordBox.pseudoClassStateChanged(FOCUSED_WITHIN, now));
    }

    @FXML
    private void handleTogglePassword() {
        boolean show = !passwordVisibleField.isVisible();

        passwordVisibleField.setVisible(show);
        passwordVisibleField.setManaged(show);
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);

        eyeIcon.setVisible(!show);
        eyeOffIcon.setVisible(show);

        TextField active = show ? passwordVisibleField : passwordField;
        active.requestFocus();
        active.positionCaret(active.getText().length());
    }

    @FXML
    private void handleLogin() {

        String username = usernameField.getText() == null
                ? ""
                : usernameField.getText().trim();

        String password = passwordField.getText() == null
                ? ""
                : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage(
                    "Please enter your username/email and password.",
                    false);

            if (username.isEmpty()) {
                usernameField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
            return;
        }

        LoginPayload payload = new LoginPayload(username, password);
        Request request = Request.of(Action.LOGIN, payload);

        try {
            ClientConnection.getInstance().send(request, response -> {
                System.out.println(response.getMessage());

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        try {
                            Navigator.goTo(Screen.HOME);
                        } catch (IOException e) {
                            showMessage("Cannot open the home screen.", false);
                            e.printStackTrace();
                        }
                    } else {
                        showMessage(response.getMessage(), false);
                        passwordField.clear();
                        passwordField.requestFocus();
                    }
                });
            });
        } catch (IOException e) {
            showMessage("Cannot send login request to server.", false);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        showMessage("Forgot password screen will be added later.", true);
    }

    @FXML
    private void handleCreateAccount() {
        showMessage("Register screen will be built after the main screens.", true);
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}
