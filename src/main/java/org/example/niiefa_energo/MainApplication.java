package org.example.niiefa_energo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


import java.io.IOException;
import java.util.Objects;

public class MainApplication extends Application {

    Notification mainController;

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("sceneMain.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        mainController = fxmlLoader.getController();
        stage.setMaximized(true);
        stage.setResizable(true);
        stage.setTitle("ЧМИ ИТ 300-15");
        stage.getIcons().add(new Image(Objects.requireNonNull(MainApplication.class.getResourceAsStream("appIcon.png"))));
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() throws Exception {
        mainController.closeApp();
        super.stop();
    }
}