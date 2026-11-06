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

import com.musicfinder.session.UserSession;
import com.musicfinder.shared.request.payload.RegisterPayload;
import com.musicfinder.shared.response.payload.UserData;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;

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
                            UserData user = response.dataAs(UserData.class);
                            UserSession.getInstance().setCurrentUser(user);
                            Navigator.goTo(Screen.HOME);
                        } catch (Exception e) {
                            showMessage("Cannot open the home screen.", false);
                            e.printStackTrace();
                        }
                    } else {
                        showMessage(response.getMessage() != null ? response.getMessage() : "Login failed.", false);
                        passwordField.clear();
                        passwordField.requestFocus();
                    }
                });
            });
        } catch (Exception e) {
            showMessage("Cannot send login request to server.", false);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        showMessage("Please contact admin or reset your password in profile settings.", true);
    }

    @FXML
    private void handleCreateAccount() {
        Dialog<RegisterPayload> dialog = new Dialog<>();
        dialog.setTitle("Create a new account");
        dialog.setHeaderText("Join Music Finder to discover and save your favorite music.");

        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField regUsername = new TextField();
        regUsername.setPromptText("Username (min 3 chars)");
        TextField regFullName = new TextField();
        regFullName.setPromptText("Full name");
        TextField regEmail = new TextField();
        regEmail.setPromptText("Email (e.g. name@example.com)");
        PasswordField regPassword = new PasswordField();
        regPassword.setPromptText("Password (min 6 chars)");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(regUsername, 1, 0);
        grid.add(new Label("Full name:"), 0, 1);
        grid.add(regFullName, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(regEmail, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(regPassword, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                return new RegisterPayload(
                        regUsername.getText().trim(),
                        regPassword.getText().trim(),
                        regFullName.getText().trim(),
                        regEmail.getText().trim()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(payload -> {
            if (payload.username().length() < 3 || payload.password().length() < 6) {
                showMessage("Username must be at least 3 chars and password at least 6 chars.", false);
                return;
            }
            try {
                Request regReq = Request.of(Action.REGISTER, payload);
                ClientConnection.getInstance().send(regReq, response -> {
                    Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            UserData user = response.dataAs(UserData.class);
                            UserSession.getInstance().setCurrentUser(user);
                            showMessage("Registration successful! Welcome " + user.userName(), true);
                            try {
                                Navigator.goTo(Screen.HOME);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            showMessage(response.getMessage() != null ? response.getMessage() : "Registration failed", false);
                        }
                    });
                });
            } catch (Exception e) {
                showMessage("Cannot connect to server for registration: " + e.getMessage(), false);
            }
        });
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}
