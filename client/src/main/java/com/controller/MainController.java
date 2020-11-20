package com.controller;

import com.client.FileService;
import com.client.Network;
import com.main.ClientMain;
import com.utils.FileInfo;
import com.utils.FileType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    private final Network network = Network.getNetwork();
    private final FileService fileService = new FileService();

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
        Thread thread = new Thread(() -> {
            while (true) {
                String msg = network.readMassage(getCurrentPath());
                if (msg.startsWith("exit\nOk")) {
                    network.closeConnection();
                    Platform.exit();
                    break;
                } else if (msg.startsWith("/updateUserList")) {
                    updateUserList(Paths.get(getCurrentPath()));
                } else if (msg.startsWith("/fileList")){
                    if (msg.split("\n").length > 1) {
                        List<FileInfo> serverList = fileService.makeFileList(msg.split("\n", 2)[1]);
                        Platform.runLater(() -> {
                            serverTable.getItems().clear();
                            serverList.forEach(element -> serverTable.getItems().add(element));
                            serverTable.sort();
                        });
                    }else {
                        Platform.runLater(() -> {
                            serverTable.getItems().clear();
                        });
                    }
                } else if (msg.startsWith("/error")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, msg.split("\n")[2], ButtonType.OK);
                    alert.setTitle(msg.split("\n")[1]);
                    alert.showAndWait();
                }
            }
        });
        thread.start();
        updateUserList(Paths.get("/"));
        updateServerList();
    }

    private void setupTableEvents(TableView<FileInfo> tableView) {
        tableView.setOnMouseClicked(event ->  {
            if (event.getClickCount() == 2) {
                if (tableView.getSelectionModel().getSelectedItem().getType() == FileType.DIRECTORY) {
                    enter();
                }
            }
            if (event.isSecondaryButtonDown()) {

            }
        });
        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                enter();
            }
            if (event.getCode() == KeyCode.DELETE) {
                delete();
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
        if (serverTable.isFocused()) {
            network.sendCommand("/enterToDirectory\n" + getSelectedFileName());
        }
    }

    private void delete() {
        if (clientTable.isFocused()) {
            Path deletedFile = getSelectedFile();
            Alert deleteAlert = new Alert(Alert.AlertType.CONFIRMATION);
            deleteAlert.setTitle("Delete");
            deleteAlert.setContentText("Are you sure you want to delete this file?");
            Optional<ButtonType> option = deleteAlert.showAndWait();
            if (option.isPresent() && option.get() == ButtonType.OK) {
                try {
                    fileService.delete(deletedFile);
                } catch (IOException exception) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR, exception.getMessage(), ButtonType.OK);
                    errorAlert.setTitle(exception.getClass().getSimpleName());
                    errorAlert.showAndWait();
                }
            }
        }
        if (serverTable.isFocused()) {
            String fileName = getSelectedFileName();
            Alert deleteAlert = new Alert(Alert.AlertType.CONFIRMATION);
            deleteAlert.setTitle("Delete");
            deleteAlert.setContentText("Are you sure you want to delete this file?");
            Optional<ButtonType> option = deleteAlert.showAndWait();
            if (option.isPresent() && option.get() == ButtonType.OK) {
                network.sendCommand("/delete\n" + fileName);
            }
        }
    }

    private Path getSelectedFile() {
        return Paths.get(getCurrentPath(), getSelectedFileName());
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
        network.sendCommand("/updateFileList\n");
    }

    public void selectDisc(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateUserList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public void userPathUp() {
        Path upperPath = Paths.get(userPathField.getText()).getParent();
        if (upperPath != null) {
            updateUserList(upperPath);
        }
    }

    private String getSelectedFileName() {
        if (clientTable.isFocused()) {
            return clientTable.getSelectionModel().getSelectedItem().getFileName();
        } else {
            return serverTable.getSelectionModel().getSelectedItem().getFileName();
        }
    }

    public void serverPathUp() {
        network.sendCommand("/upDirectory");
    }

    public void Exit() {
        network.sendCommand("exit");
    }

    private String getCurrentPath() {
        return userPathField.getText();
    }

    public void upload() {
        if (clientTable.isFocused()) {
            try {
                network.sendFile(getSelectedFile(), Files.size(getSelectedFile()));
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void download() {
        if (serverTable.isFocused()) {
            network.sendCommand("/download\n" + getSelectedFileName());
        }
    }

    public void toAuthMenu() {
        ClientMain.getInstance().toAuthorizationScreen();
    }

    public void info() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Info");
        info.setContentText("Here you will find information about the program");
        info.showAndWait();
    }
}
