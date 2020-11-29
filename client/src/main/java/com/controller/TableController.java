package com.controller;

import com.utils.FileInfo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.nio.file.*;
import java.time.format.DateTimeFormatter;

public class TableController{
    public static void makeTable(TableView<FileInfo> tableView) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>("Type");
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
//        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileTypeColumn.setPrefWidth(20);
        fileTypeColumn.setCellFactory(column -> new TableCell<FileInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                System.out.println(item);
                super.updateItem(item, empty);
                setText(null);
                if (item == null || empty) {
                    setStyle("");
                    setGraphic(null);
                } else {
                    ImageView iv = new ImageView("images/file.png");
                    iv.setFitHeight(15);
                    iv.setFitWidth(15);
                    if (item.equals("U")) {
                        iv.setImage(new Image("images/up.png"));
                    }
                    if (item.equals("D")) {
                        iv.setImage(new Image("images/folder.png"));
                    }
                    setGraphic(iv);
                }
            }
        });

        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileNameColumn.setPrefWidth(235);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(130);
        fileSizeColumn.setCellFactory(column -> new TableCell<FileInfo, Long>(){
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    String text = String.format("%,d bytes", item);
                    if (item == -1) {
                        text = ("[DIR]");
                    }
                    setText(text);
                }
            }
        });

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Date modified");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(formatter)));
        fileDateColumn.setPrefWidth(115);

        tableView.getColumns().addAll(fileTypeColumn, fileNameColumn, fileSizeColumn, fileDateColumn);
        tableView.getSortOrder().add(fileTypeColumn);
    }

    public static void makeComboBox(ComboBox<String> comboBox) {
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            comboBox.getItems().add(p.toString());
        }
        comboBox.getSelectionModel().select(0);
    }
}
