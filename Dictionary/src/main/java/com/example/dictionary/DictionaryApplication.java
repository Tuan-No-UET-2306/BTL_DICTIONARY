package com.example.dictionary;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class DictionaryApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("main.fxml")));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BaChuTeEnglish");
        Image icon = new Image
                ("D:\\BaiTapLon\\Dictionary\\src\\main\\resources\\picture\\LoGo\\icon.png");
        primaryStage.getIcons().add(icon);
        primaryStage.show();
    }
}
