package org.phasorj.ui;

import java.io.File;

import

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class PhasorJ extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        //load scence
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/phasorj/plugin-layout.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}