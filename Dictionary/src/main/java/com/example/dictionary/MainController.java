package com.example.dictionary;

import Function.ChangeStage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainController implements EventHandler<ActionEvent> {
    @FXML
    Button homeButton;
    @FXML
    Button dicButton;
    @FXML
    Button transButton;
    @FXML
    Button gameButton;

//    public void initialize() {
//        homeButton.setOnAction(e -> {
//            ChangeStage.changeStage(homeButton, "home_tab.fxml", getClass());
//        });
//    }
    @Override
    public void handle(ActionEvent event) {
        homeButton.setOnAction(e -> {
//            try {
//                Parent nextStage = FXMLLoader.load(Objects.requireNonNull
//                        (getClass().getResource("home_tab.fxml")));
//                Scene nextScene = new Scene(nextStage);
//
//                // Tạo Stage mới
//                Stage newStage = new Stage();
//                newStage.setScene(nextScene);
//                newStage.setTitle("HomeTab");
//                newStage.show();
//
//                // Đóng Stage cũ
//                Stage currentStage = (Stage) homeButton.getScene().getWindow();
//                currentStage.close();
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
            ChangeStage.changeStage(homeButton, "home_tab.fxml", getClass());

        });
        dicButton.setOnAction(e -> {
//            try {
//                Parent nextStage = FXMLLoader.load(Objects.requireNonNull
//                        (getClass().getResource("main_dictionary.fxml")));
//                Scene nextScene = new Scene(nextStage);
//
//                // Tạo Stage mới
//                Stage newStage = new Stage();
//                newStage.setScene(nextScene);
//                newStage.setTitle("Dictionary");
//                newStage.show();
//
//                // Đóng Stage cũ
//                Stage currentStage = (Stage) dicButton.getScene().getWindow();
//                currentStage.close();
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
            ChangeStage.changeStage(dicButton,"main_dictionary.fxml", getClass());
        });
        gameButton.setOnAction(e -> {
//            try {
//                Parent nextStage = FXMLLoader.load(Objects.requireNonNull
//                        (getClass().getResource("game_tab.fxml")));
//                Scene nextScene = new Scene(nextStage);
//
//                // Tạo Stage mới
//                Stage newStage = new Stage();
//                newStage.setScene(nextScene);
//                newStage.setTitle("Game");
//                newStage.show();
//
//                // Đóng Stage cũ
//                Stage currentStage = (Stage) gameButton.getScene().getWindow();
//                currentStage.close();
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
            ChangeStage.changeStage(gameButton,"game_tab.fxml", getClass());
        });
        transButton.setOnAction(e -> {
//            try {
//                Parent nextStage = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("google_tab.fxml")));
//                Scene nextScene = new Scene(nextStage);
//
//                // Tạo Stage mới
//                Stage newStage = new Stage();
//                newStage.setScene(nextScene);
//                newStage.setTitle("Google Tab");
//                newStage.show();
//
//                // Đóng Stage cũ
//                Stage currentStage = (Stage) transButton.getScene().getWindow();
//                currentStage.close();
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
//        });
            ChangeStage.changeStage(transButton,"google_tab.fxml", getClass());
        });
}
}
