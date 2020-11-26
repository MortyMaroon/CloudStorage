package com.controller;

import com.client.FileService;
import com.client.Network;
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
    private final Network network = Network.getNetwork();
    private final FileService fileService = new FileService();

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
            while (true) {
                String str = network.readMassage();
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
                if (str.startsWith("exit\nOk")) {
                    network.closeConnection();
                    Platform.exit();
                    break;
                }
            }
        });
        thread.start();
    }

    public void signIN() {
        if (!Login.getText().isEmpty() && !Password.getText().isEmpty()) {
            try {
                fileService.sendCommand(network.getOutputStream(), "/auth\n" + Login.getText() + "\n" + Password.getText());
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
                    fileService.sendCommand(network.getOutputStream(), "/reg\n" + NewLogin.getText() + "\n" + NewPassword.getText());
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
        if (network.getStatus()) {
            try {
                fileService.sendCommand(network.getOutputStream(), "exit");
            } catch (IOException exception) {
                createAlert(exception);
            }
        } else {
            Platform.exit();
        }
    }

    private void createAlert(Exception exception) {
        Alert errorAlert = new Alert(Alert.AlertType.WARNING, exception.getMessage(), ButtonType.OK);
        errorAlert.setTitle(exception.getClass().getSimpleName());
        errorAlert.showAndWait();
    }
}
