package com.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller {

    @FXML
    VBox client, server;

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }


    public void copyBtnAction(ActionEvent actionEvent) {
        PanelController client = (PanelController) this.client.getProperties().get("com/controller");
        PanelController server = (PanelController) this.server.getProperties().get("com/controller");
        if (client.getSelectedFilename() == null && server.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        PanelController srcPC = null, dstPC = null;
        if (client.getSelectedFilename() != null) {
            srcPC = client;
            dstPC = server;
        }
        if (server.getSelectedFilename() != null) {
            srcPC = server;
            dstPC = client;
        }
        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());
        Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
        try {
            Files.copy(srcPath,dstPath);
            dstPC.updateList(Paths.get(dstPC.getCurrentPath()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
            alert.showAndWait();
        }
    }

}
