package org.example.niiefa_energo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


import java.io.IOException;
import java.util.Objects;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("sceneMain.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1366, 768);
        fxmlLoader.getController();
        stage.setMinHeight(768);
        stage.setMinWidth(1366);
        stage.setMaximized(false);
        stage.setResizable(false);
        stage.setTitle("Программа для исследования токовых зависимостей");
        stage.getIcons().add(new Image(Objects.requireNonNull(MainApplication.class.getResourceAsStream("appIcon.png"))));
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }

}