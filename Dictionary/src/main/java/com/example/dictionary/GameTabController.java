package com.example.dictionary;

import Function.ChangeStage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class GameTabController implements EventHandler<ActionEvent> {
    @FXML
    public Button quizzButton;
    @FXML
    public Button ctwButton;
    @FXML
    public Button exitGame;


    @Override
    public void handle(ActionEvent event) {
        exitGame.setOnAction(e -> {
            ChangeStage.changeStage(exitGame, "main.fxml", getClass());
        });
        ctwButton.setOnAction(e -> {
            ChangeStage.changeStage(ctwButton,"wordlegame.fxml",getClass());
        });
        quizzButton.setOnAction(e -> {
            ChangeStage.changeStage(quizzButton,"quizz_game.fxml",getClass());
        });
    }

}