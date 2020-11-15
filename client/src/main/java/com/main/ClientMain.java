package com.main;

import com.client.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ClientMain extends Application {
    private Stage stage;
    private static ClientMain instanceClient;
    private static Network instanceNetwork;
    private double xOffset = 0;
    private double yOffset = 0;

    public void toMainScreen() {
        try {
            chooseScene("/client.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toAuthorizationScreen() {
        try {
            chooseScene("/authorization.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Parent chooseScene(String fxml) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(root);
        }
        stage.sizeToScene();
        return root;
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Scene scene = new Scene(root);
        stage = primaryStage;
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        root.setOnMousePressed((MouseEvent event) -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged((MouseEvent event) -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });
        toAuthorizationScreen();
        primaryStage.show();
    }


    public static void main(String[] args) {
        instanceNetwork = new Network("localhost", 8189);
        launch(args);
    }
}
