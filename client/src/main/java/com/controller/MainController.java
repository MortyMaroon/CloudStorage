package com.controller;

import com.client.Network;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    private Network network = Network.getNetwork();

    @FXML
    private ComboBox<String> disksBox;

    @FXML
    private TextField userPathField, serverPathField;

    @FXML
    private TableView<FileInfo> clientTable, serverTable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableController.makeTable(clientTable);
        TableController.makeComboBox(disksBox);
        TableController.makeTable(serverTable);
        setupTableEvents(clientTable);
        setupTableEvents(serverTable);
//        Thread thread = new Thread(() -> {
//            while (true) {
//                String str = network.readMassage();
//                if (str.startsWith("/auth\nok")) {
//                    Platform.runLater(this::changeMainScreen);
//                    break;
//                }
//                if (str.startsWith("/auth\nnoSuch")) {
//                    Platform.runLater(this::setLogLabel);
//                }
//                if (str.startsWith("/login\nbusy")) {
//                    Platform.runLater(this::setRegLabel);
//                }
//                if (str.startsWith("exitOk")) {
//                    network.closeConnection();
//                    Platform.exit();
//                    break;
//                }
//            }
//        });
//        thread.start();
        updateUserList(Paths.get("/"));
        updateServerList();
    }

    private void setupTableEvents(TableView<FileInfo> tableView) {
        tableView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    if (tableView.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.DIRECTORY) {
                        enter();
                    }
                }
            }
        });
    }

    private void enter() {
        if (clientTable.isFocused()) {
            Path path = Paths.get(userPathField.getText())
                    .resolve(clientTable.
                            getSelectionModel()
                            .getSelectedItem()
                            .getFileName());
            if (Files.isDirectory(path)) {
                updateUserList(path);
            }
        }
    }

    private void updateUserList(Path path) {
        try {
            userPathField.setText(path.normalize().toAbsolutePath().toString());
            clientTable.getItems().clear();
            clientTable.getItems()
                    .addAll(Files.list(path)
                            .map(FileInfo::new)
                            .collect(Collectors.toList()));
            clientTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "failed to update the file list", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void updateServerList() {

    }

    public void selectDisc(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateUserList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public void userPathUp(ActionEvent actionEvent) {
        Path upperPath = Paths.get(userPathField.getText()).getParent();
        if (upperPath != null) {
            updateUserList(upperPath);
        }
    }

    public void serverPathUp(ActionEvent actionEvent) {

    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }
}
