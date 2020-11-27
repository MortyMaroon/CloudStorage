package com.controller;

import com.client.Network;
import com.client.NetworkReaderWriter;
import com.main.ClientMain;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthController implements Initializable {
    private final NetworkReaderWriter networkReaderWriter = new NetworkReaderWriter();
    private final Network network = Network.getNetwork();

    @FXML
    private TextField Login, NewLogin;
    @FXML
    private PasswordField Password, NewPassword, ConfPassword;
    @FXML
    private Label SignInMessage, SignUpMessage;
    @FXML
    private Pane pnlReg, pnlSignIn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    String str = networkReaderWriter.readFromNetwork(network.getInputStream());
                    if (str.startsWith("/auth\nok")) {
                        Platform.runLater(this::changeMainScreen);
                        break;
                    }
                    if (str.startsWith("/auth\nnoSuch")) {
                        Platform.runLater(this::setLogLabel);
                    }
                    if (str.startsWith("/login\nbusy")) {
                        Platform.runLater(this::setRegLabel);
                    }
                }
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    createAlert(exception);
                    network.closeConnection();
                    Platform.exit();
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void signIN() {
        if (!Login.getText().isEmpty() && !Password.getText().isEmpty()) {
            try {
                networkReaderWriter.writeToNetwork(network.getOutputStream(), "/auth\n" + Login.getText() + "\n" + Password.getText());
                Login.clear();
                Password.clear();
            } catch (IOException exception) {
                createAlert(exception);
            }
        } else {
            SignInMessage.setText("Please enter login and password.");
        }
    }

    public void signUp() {
        SignUpMessage.setText("");
        if (!NewPassword.getText().isEmpty() && !ConfPassword.getText().isEmpty() && !NewLogin.getText().isEmpty()) {
            if (!NewPassword.getText().equals(ConfPassword.getText())) {
                SignUpMessage.setText("Passwords do not match.");
            } else {
                try {
                    networkReaderWriter.writeToNetwork(network.getOutputStream(), "/reg\n" + NewLogin.getText() + "\n" + NewPassword.getText());
                } catch (IOException exception) {
                    createAlert(exception);
                }
            }
        } else {
            SignUpMessage.setText("Please enter all data.");
        }
        NewLogin.clear();
        NewPassword.clear();
        ConfPassword.clear();
    }

    public void changeSignUpMenu() {
        pnlReg.toFront();
        pnlReg.setVisible(true);
        pnlReg.setDisable(false);
        pnlSignIn.setDisable(true);
        pnlSignIn.setVisible(false);
    }

    public void changeSignInMenu() {
        pnlSignIn.toFront();
        pnlSignIn.setVisible(true);
        pnlSignIn.setDisable(false);
        pnlReg.setDisable(true);
        pnlReg.setVisible(false);
    }

    private void changeMainScreen() {
        ClientMain.getInstance().toMainScreen();
    }

    private void setLogLabel() {
        SignInMessage.setText("Invalid Login. Please try again.");
    }

    private void setRegLabel() {
        SignUpMessage.setText("Login is busy.");
    }

    public void exit() {
        try {
            networkReaderWriter.writeToNetwork(network.getOutputStream(), "exit");
            network.closeConnection();
        } catch (IOException exception) {
            createAlert(exception);
        } finally {
            Platform.exit();
        }
    }

    private void createAlert(Exception exception) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR, exception.getMessage(), ButtonType.OK);
        errorAlert.setTitle(exception.getClass().getSimpleName());
        errorAlert.showAndWait();
    }
}
