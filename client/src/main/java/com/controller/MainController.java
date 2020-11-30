package com.controller;

import com.client.WindowService;
import com.utils.FileService;
import com.client.Network;
import com.client.NetworkReaderWriter;
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
    private final NetworkReaderWriter networkReaderWriter = new NetworkReaderWriter();
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
            try {
                while (true) {
                    String msg = networkReaderWriter.readFromNetwork(network.getInputStream());
                    if (msg.startsWith("/updateUserList")) {
                        updateUserList(Paths.get(getCurrentPath()));
                    }
                    if (msg.startsWith("exit\nOk")) {
                        network.closeConnection();
                        Platform.exit();
                        break;
                    }
                    if (msg.startsWith("/disconnect\nOk")) {
                        Platform.runLater(() -> ClientMain.getInstance().toAuthorizationScreen());
                        break;
                    }
                    if (msg.startsWith("/error")) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR, msg.split("\n")[2], ButtonType.OK);
                            alert.setTitle(msg.split("\n")[1]);
                            alert.showAndWait();
                        });
                    }
                    if (msg.startsWith("/serverPath")) {
                        Platform.runLater(() -> serverPathField.setText(msg.split("\n")[1]));
                    }
                    if (msg.startsWith("/fileList")){
                        if (msg.split("\n").length > 1) {
                            List<FileInfo> serverList = fileService.makeFileList(msg.split("\n", 2)[1]);
                            Platform.runLater(() -> {
                                serverTable.getItems().clear();
                                serverList.forEach(element -> serverTable.getItems().add(element));
                                serverTable.sort();
                            });
                        }else {
                            Platform.runLater(() -> serverTable.getItems().clear());
                        }
                    }
                }
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    WindowService.createAlert(exception);
                    network.closeConnection();
                    Platform.exit();
                });
            }
        });
        thread.setDaemon(true);
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
        });
        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (tableView.getSelectionModel().getSelectedItem().getType() == FileType.DIRECTORY) {
                    enter();
                }
            } else if (event.getCode() == KeyCode.DELETE) {
                if (tableView.getSelectionModel().getSelectedItem().getType() == FileType.FILE) {
                    delete();
                }
            }
        });
    }

    private void enter() {
        if (clientTable.isFocused()) {
            updateUserList(Paths.get(userPathField.getText()).resolve(getSelectedFileName()));
        } else {
            try {
                networkReaderWriter.writeToNetwork(network.getOutputStream() ,"/enterToDirectory\n" + getSelectedFileName());
            } catch (IOException exception) {
                WindowService.createAlert(exception);
            }
        }
    }

    private void delete() {
        if (clientTable.isFocused()) {
            if (clientTable.getSelectionModel().getSelectedItem().getType() == FileType.FILE) {
                Optional<ButtonType> option = WindowService.alertOption("Delete", "Are you sure you want to delete this file?");
                if (option.isPresent() && option.get() == ButtonType.OK) {
                    try {
                        fileService.delete(getSelectedFile());
                    } catch (IOException exception) {
                        WindowService.createAlert(exception);
                    }
                    updateUserList(Paths.get(getCurrentPath()));
                }
            }
        }
        if (serverTable.isFocused()){
            if (serverTable.getSelectionModel().getSelectedItem().getType() == FileType.FILE) {
                Optional<ButtonType> option = WindowService.alertOption("Delete", "Are you sure you want to delete this file?");
                if (option.isPresent() && option.get() == ButtonType.OK) {
                    try {
                        networkReaderWriter.writeToNetwork(network.getOutputStream(), "/delete\n" + getSelectedFileName());
                    } catch (IOException exception) {
                        WindowService.createAlert(exception);
                    }
                }
            }
        }
    }

    private void renameFile() {
        if (clientTable.isFocused()) {
            if (clientTable.getSelectionModel().getSelectedItem().getType() == FileType.FILE) {
                Optional<String> result = WindowService.dialogOption("Rename file", "Enter a new name for the file");
                if (result.isPresent()) {
                    try {
                        fileService.renameFile(Paths.get(userPathField.getText()).resolve(getSelectedFileName()), getSelectedFileName(), result.get());
                    } catch (Exception exception) {
                        WindowService.createAlert(exception);
                    }
                }
            }
        }
        if (serverTable.isFocused()) {
            if (serverTable.getSelectionModel().getSelectedItem().getType() == FileType.FILE) {
                Optional<String> result = WindowService.dialogOption("Rename file", "Enter a new name for the file");
                if (result.isPresent()) {
                    try {
                        networkReaderWriter.writeToNetwork(network.getOutputStream(), String.format("/rename\n%s\n%s", getSelectedFileName(), result.get()));
                    } catch (Exception exception) {
                        WindowService.createAlert(exception);
                    }
                }
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
        try {
            networkReaderWriter.writeToNetwork(network.getOutputStream(), "/updateFileList\n");
        } catch (IOException exception) {
            WindowService.createAlert(exception);
        }
    }

    public void selectDisc(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateUserList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    private String getSelectedFileName() {
        if (clientTable.isFocused()) {
            return clientTable.getSelectionModel().getSelectedItem().getFileName();
        } else {
            return serverTable.getSelectionModel().getSelectedItem().getFileName();
        }
    }

    public void Exit() {
        try {
            networkReaderWriter.writeToNetwork(network.getOutputStream(), "exit\n");
        } catch (IOException exception) {
            WindowService.createAlert(exception);
            network.closeConnection();
            Platform.exit();
        }
    }

    private String getCurrentPath() {
        return userPathField.getText();
    }

    public void toAuthMenu() {
        try {
            networkReaderWriter.writeToNetwork(network.getOutputStream(), "/disconnect\n");
        } catch (IOException exception) {
            WindowService.createAlert(exception);
        }
    }

    public void info() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Info");
        info.setContentText("Here you will find information about the program");
        info.showAndWait();
    }

    public void sendFile() {
        if (clientTable.isFocused()) {
            try {
                fileService.sendFile(network.getOutputStream(), getSelectedFile());
            } catch (IOException exception) {
                WindowService. createAlert(exception);
            }
        }
    }

    public void updateUserTable() {
        updateUserList(Paths.get(getCurrentPath()));
    }

    public void backUser() {
        Path upperPath = Paths.get(userPathField.getText()).getParent();
        if (upperPath != null) {
            updateUserList(upperPath);
        }
    }

    public void toHomeUser() {
        updateUserList(Paths.get("/"));
    }

    public void renameUserFile() {
        renameFile();
    }

    public void deleteUserFile() {
        delete();
    }

    public void downloadFile() {
        if (serverTable.isFocused()) {
            try {
                networkReaderWriter.writeToNetwork(network.getOutputStream(), String.format("/download\n%s\n%s", getSelectedFileName(), getCurrentPath()));
            } catch (IOException exception) {
                WindowService.createAlert(exception);
            }
        }
    }

    public void updateServerTable() {
        updateServerList();
    }

    public void backServer() {
        try {
            networkReaderWriter.writeToNetwork(network.getOutputStream(), "/upDirectory\n");
        } catch (IOException exception) {
            WindowService.createAlert(exception);
        }
    }

    public void toHomeServer() {
        try {
            networkReaderWriter.writeToNetwork(network.getOutputStream(), "/toHomeDirectory\n");
        } catch (IOException exception) {
            WindowService.createAlert(exception);
        }
    }

    public void renameServerFile() {
        renameFile();
    }

    public void deleteServerFile() {
        delete();
    }

    public void createServerFolder() {
        Optional<String> result = WindowService.dialogOption("Create new folder", "Enter a name for the new folder");
        if (result.isPresent()) {
            try {
                networkReaderWriter.writeToNetwork(network.getOutputStream(), "/mkdir\n" + result.get());
            } catch (IOException exception) {
                WindowService.createAlert(exception);
            }
        }
    }

    public void createServerFile() {
        Optional<String> result = WindowService.dialogOption("Create new file", "Enter a name for the new file");
        if (result.isPresent()) {
            try {
                networkReaderWriter.writeToNetwork(network.getOutputStream(), "/createFile\n" + result.get());
            } catch (IOException exception) {
                WindowService.createAlert(exception);
            }
        }
    }

    public void createUserFolder() {
        Optional<String> result = WindowService.dialogOption("Create new folder","Enter a name for the new folder");
        if (result.isPresent()) {
            try {
                fileService.createDirectory(Paths.get(getCurrentPath()), result.get());
            } catch (Exception exception) {
                WindowService.createAlert(exception);
            } finally {
                updateUserList(Paths.get(getCurrentPath()));
            }
        }
    }

    public void createUserFile() {
        Optional<String> result = WindowService.dialogOption("Create new file","Enter a name for the new file");
        if (result.isPresent()) {
            try {
                fileService.createFile(Paths.get(getCurrentPath()), result.get());
            } catch (Exception exception) {
                WindowService.createAlert(exception);
            } finally {
                updateUserList(Paths.get(getCurrentPath()));
            }
        }
    }
}