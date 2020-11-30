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
    private static Image audio = new Image("images/files/audio.png");
    private static Image doc = new Image("images/files/doc.png");
    private static Image excel = new Image("images/files/excel.png");
    private static Image folder = new Image("images/files/folder.png");
    private static Image image = new Image("images/files/image.png");
    private static Image pdf = new Image("images/files/pdf.png");
    private static Image text = new Image("images/files/text.png");
    private static Image video = new Image("images/files/video.png");
    private static Image word = new Image("images/files/word.png");
    private static Image zip = new Image("images/files/zip.png");

    public static void makeTable(TableView<FileInfo> tableView) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>("Type");
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileTypeColumn.setPrefWidth(20);
        fileTypeColumn.setCellFactory(column -> new TableCell<FileInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                setText(null);
                if (item == null || empty) {
                    setStyle("");
                    setGraphic(null);
                } else {
                    ImageView imageView = new ImageView(doc);
                    imageView.setFitHeight(15);
                    imageView.setFitWidth(15);
                    if (!item.contains(".")) {
                        imageView.setImage(folder);
                    }
                    if (item.matches(".*\\.docx$")) {
                        imageView.setImage(word);
                    }
                    if (item.matches(".*\\.(png|jpg|jpeg|gif|tiff|psd)$")) {
                        imageView.setImage(image);
                    }
                    if (item.matches(".*\\.(mp4|mkv)$")) {
                        imageView.setImage(video);
                    }
                    if (item.matches(".*\\.mp3$")) {
                        imageView.setImage(audio);
                    }
                    if (item.matches(".*\\.pdf$")) {
                        imageView.setImage(pdf);
                    }
                    if (item.matches(".*\\.txt$")) {
                        imageView.setImage(text);
                    }
                    if (item.matches(".*\\.xlsx$")) {
                        imageView.setImage(excel);
                    }
                    if (item.matches(".*\\.(zip|rar)$")) {
                        imageView.setImage(zip);
                    }
                    setGraphic(imageView);
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
