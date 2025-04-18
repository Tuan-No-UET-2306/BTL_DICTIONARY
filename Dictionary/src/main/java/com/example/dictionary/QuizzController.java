package com.example.dictionary;

import Function.ChangeStage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;


public class QuizzController implements EventHandler<ActionEvent> {
    @FXML
    Button exitQuizz;
    @FXML
    Label quesLable;
    @FXML
    Button ansA;
    @FXML
    Button ansC;
    @FXML
    Button ansD;
    @FXML
    Button ansB;


    @Override
    public void handle(ActionEvent event) {
        exitQuizz.setOnAction(e -> {
            ChangeStage.changeStage(exitQuizz, "game_tab.fxml", getClass());
        });
        ansA.setOnAction(e ->{
            try {
                Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("win.fxml")));
                Stage stage = new Stage();
                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                stage.setScene(scene);
                stage.setTitle("BaChuTeEnglish");
                stage.initStyle(StageStyle.UNDECORATED);
                stage.initStyle(StageStyle.TRANSPARENT); // <-- Sửa chỗ này
                stage.show();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

    }
}
