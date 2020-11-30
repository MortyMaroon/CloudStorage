package com.client;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class WindowService {

    public static void createAlert(Exception exception) {
        Alert errorAlert = new Alert(Alert.AlertType.CONFIRMATION, exception.getMessage(), ButtonType.OK);
        errorAlert.setTitle(exception.getClass().getSimpleName());
        errorAlert.showAndWait();
    }

    public static Optional<String> dialogOption(String title, String text) {
        TextInputDialog window = new TextInputDialog();
        window.setTitle(title);
        window.setHeaderText(text);
        return window.showAndWait();
    }

    public static Optional<ButtonType> alertOption(String title, String text) {
        Alert deleteAlert = new Alert(Alert.AlertType.CONFIRMATION);
        deleteAlert.setTitle(title);
        deleteAlert.setContentText(text);
        return deleteAlert.showAndWait();
    }
}
