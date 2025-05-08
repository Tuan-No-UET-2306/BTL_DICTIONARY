package com.example.dictionary; // Đảm bảo đúng package

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class ResultNotificationManager {

    private VBox resultNotificationVBox;
    private final AnchorPane rootPane; // To add/remove the VBox from
    private final Runnable onNextQuestionAction; // Callback for "NEXT" on non-last question
    private final Runnable onQuizEndAction;      // Callback for "NEXT" on last question

    private boolean wasLastQuestion; // Stores if the notification was for the last question

    public ResultNotificationManager(AnchorPane rootPane, Runnable onNextQuestionAction, Runnable onQuizEndAction) {
        if (rootPane == null) {
            throw new IllegalArgumentException("rootPane cannot be null for ResultNotificationManager");
        }
        this.rootPane = rootPane;
        this.onNextQuestionAction = onNextQuestionAction;
        this.onQuizEndAction = onQuizEndAction;
    }

    public void show(boolean isCorrect, String correctAnswerText, boolean isLastQuestion) {
        // If already showing, remove the old one first (or prevent showing a new one)
        if (resultNotificationVBox != null) {
            hide();
        }

        this.wasLastQuestion = isLastQuestion; // Store this for the button action

        resultNotificationVBox = new VBox(15);
        resultNotificationVBox.setAlignment(Pos.CENTER);
        resultNotificationVBox.setPadding(new Insets(25, 30, 25, 30));

        Label titleMsgLabel = new Label();
        Label detailsMsgLabel = new Label();
        Button notificationNextButton = new Button("NEXT");

        if (isCorrect) {
            titleMsgLabel.setText("CHÚC MỪNG!");
            detailsMsgLabel.setText("Bạn đã trả lời đúng!\nĐáp án chính xác là: " + correctAnswerText);
            resultNotificationVBox.getStyleClass().add("result-vbox-correct");
        } else {
            titleMsgLabel.setText("RẤT TIẾC!");
            detailsMsgLabel.setText("Bạn đã trả lời sai.\nĐáp án đúng là: " + correctAnswerText);
            resultNotificationVBox.getStyleClass().add("result-vbox-incorrect");
        }
        resultNotificationVBox.getStyleClass().add("result-vbox");
        titleMsgLabel.getStyleClass().add("result-title");
        detailsMsgLabel.getStyleClass().add("result-details");
        notificationNextButton.getStyleClass().add("result-next-button");

        notificationNextButton.setOnAction(e -> {
            hide(); // Always hide the notification first
            if (this.wasLastQuestion) {
                if (onQuizEndAction != null) {
                    onQuizEndAction.run();
                }
            } else {
                if (onNextQuestionAction != null) {
                    onNextQuestionAction.run();
                }
            }
        });

        resultNotificationVBox.getChildren().addAll(titleMsgLabel, detailsMsgLabel, notificationNextButton);

        rootPane.getChildren().add(resultNotificationVBox);
        AnchorPane.setTopAnchor(resultNotificationVBox, 150.0);
        AnchorPane.setLeftAnchor(resultNotificationVBox, 200.0);
        AnchorPane.setRightAnchor(resultNotificationVBox, 200.0);
    }

    public void hide() {
        if (resultNotificationVBox != null && rootPane.getChildren().contains(resultNotificationVBox)) {
            rootPane.getChildren().remove(resultNotificationVBox);
            resultNotificationVBox = null;
        }
    }

    public boolean isShowing() {
        return resultNotificationVBox != null;
    }
}