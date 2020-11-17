package com.controller;

import com.client.Network;
import com.main.ClientMain;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthController implements Initializable {
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
                if (str.startsWith("exitOk")) {
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
            network.sendMassage("/auth " + Login.getText() + " " + Password.getText());
            Login.clear();
            Password.clear();
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
                network.sendMassage("/reg " + NewLogin.getText() + " " + NewPassword.getText());
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
            network.sendMassage("exit");
        } else {
            Platform.exit();
        }
    }
}