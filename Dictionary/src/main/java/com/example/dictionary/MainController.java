package com.example.dictionary;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javax.swing.*;

public class MainController {
    @FXML
    Button b1;
    @FXML
    Label l1;

    public void setL1(ActionEvent event) {
        l1.setText("abc");

        System.out.println("abc");
    }


}
