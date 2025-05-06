package com.example.dictionary;

import Function.ChangeStage;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.awt.event.ActionEvent;

public class HomeTabController {
    @FXML
    Button backButton;

    public void handleBack(javafx.event.ActionEvent event) {
        ChangeStage.changeStage(backButton, "main.fxml", getClass());
    }
}
