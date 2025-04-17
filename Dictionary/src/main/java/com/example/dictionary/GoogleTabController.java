package com.example.dictionary;

import Function.ChangeStage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class GoogleTabController implements EventHandler<ActionEvent> {
    @FXML
    public Button exitTrans;
    @FXML
    public Button hoanDoiButton;

    @Override
    public void handle(ActionEvent event) {
        exitTrans.setOnAction(e -> {
            ChangeStage.changeStage(exitTrans, "main.fxml", getClass());
        });

    }



}
