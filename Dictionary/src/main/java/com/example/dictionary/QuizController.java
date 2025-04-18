package com.example.dictionary;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class QuizController implements Initializable {

    @FXML
    private VBox quizPane; // Container chính

    @FXML
    private Label questionLabel;

    @FXML
    private Button answerButton1;

    @FXML
    private Button answerButton2;

    @FXML
    private Button answerButton3;

    @FXML
    private Button answerButton4;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label scoreLabel;

    @FXML
    private Button nextButton;

    @FXML
    private VBox resultsPane; // Container hiển thị kết quả

    @FXML
    private Label finalScoreLabel;

    @FXML
    private Button restartButton;

    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private Button selectedButton = null; // Lưu trữ nút đã chọn

    // Lớp nội bộ để biểu diễn một câu hỏi
    private static class Question {
        String questionText;
        List<String> options;
        int correctAnswerIndex; // Index của đáp án đúng trong list options (0-3)

        Question(String questionText, List<String> options, int correctAnswerIndex) {
            this.questionText = questionText;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupQuestions();
        loadQuestion();
        resultsPane.setVisible(false); // Ẩn phần kết quả ban đầu
        resultsPane.setManaged(false); // Không quản lý layout cho phần kết quả
        nextButton.setDisable(true);   // Vô hiệu hóa nút Next ban đầu
    }

    private void setupQuestions() {
        // Tạo danh sách câu hỏi mẫu
        questions = new ArrayList<>();
        questions.add(new Question("Ngôn ngữ lập trình nào phổ biến nhất cho Android?",
                Arrays.asList("Swift", "Java", "Kotlin", "C#"), 1)); // Java là đáp án đúng (index 1)
        questions.add(new Question("HTML là viết tắt của?",
                Arrays.asList("Hyper Trainer Marking Language", "Hyper Text Marketing Language", "Hyper Text Markup Language", "Hyperlink and Text Markup Language"), 2));
        questions.add(new Question("CSS dùng để làm gì?",
                Arrays.asList("Định nghĩa cấu trúc trang web", "Định dạng và tạo kiểu cho trang web", "Xử lý logic phía client", "Quản lý cơ sở dữ liệu"), 1));
        questions.add(new Question("Thủ đô của Việt Nam là gì?",
                Arrays.asList("TP. Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Hà Nội"), 3));
        questions.add(new Question("JavaFX dùng để làm gì?",
                Arrays.asList("Phát triển Web Backend", "Phát triển ứng dụng Desktop", "Phát triển Game Mobile", "Phân tích dữ liệu"), 1));


        // Xáo trộn câu hỏi (tùy chọn)
        Collections.shuffle(questions);
    }

    private void loadQuestion() {
        if (currentQuestionIndex < questions.size()) {
            Question currentQuestion = questions.get(currentQuestionIndex);
            questionLabel.setText("Câu " + (currentQuestionIndex + 1) + ": " + currentQuestion.questionText);
            List<String> options = currentQuestion.options;
            answerButton1.setText(options.get(0));
            answerButton2.setText(options.get(1));
            answerButton3.setText(options.get(2));
            answerButton4.setText(options.get(3));

            // Reset trạng thái các nút và feedback
            resetButtonsStyle();
            feedbackLabel.setText("");
            feedbackLabel.getStyleClass().removeAll("feedback-correct", "feedback-incorrect");
            selectedButton = null;
            nextButton.setDisable(true); // Vô hiệu hóa nút Next khi tải câu mới
            enableAnswerButtons(true); // Kích hoạt lại các nút trả lời

        } else {
            // Kết thúc quiz
            showResults();
        }
    }

    @FXML
    void handleAnswer(ActionEvent event) {
        // Vô hiệu hóa các nút trả lời sau khi chọn
        enableAnswerButtons(false);

        // Xác định nút được nhấn
        selectedButton = (Button) event.getSource();
        int selectedAnswerIndex = -1;

        if (selectedButton == answerButton1) selectedAnswerIndex = 0;
        else if (selectedButton == answerButton2) selectedAnswerIndex = 1;
        else if (selectedButton == answerButton3) selectedAnswerIndex = 2;
        else if (selectedButton == answerButton4) selectedAnswerIndex = 3;

        // Kiểm tra đáp án
        Question currentQuestion = questions.get(currentQuestionIndex);
        boolean isCorrect = (selectedAnswerIndex == currentQuestion.correctAnswerIndex);

        // Hiển thị feedback và tô màu nút
        feedbackLabel.getStyleClass().removeAll("feedback-correct", "feedback-incorrect"); // Xóa class cũ
        if (isCorrect) {
            feedbackLabel.setText("Chính xác!");
            feedbackLabel.getStyleClass().add("feedback-correct");
            selectedButton.getStyleClass().add("correct-answer"); // Tô màu xanh cho nút đúng
            score++; // Tăng điểm
        } else {
            feedbackLabel.setText("Sai rồi! Đáp án đúng là: " + currentQuestion.options.get(currentQuestion.correctAnswerIndex));
            feedbackLabel.getStyleClass().add("feedback-incorrect");
            selectedButton.getStyleClass().add("incorrect-answer"); // Tô màu đỏ cho nút sai

            // Tìm và tô màu xanh cho nút đúng
            Button correctButton = getButtonByIndex(currentQuestion.correctAnswerIndex);
            if(correctButton != null) {
                correctButton.getStyleClass().add("correct-answer");
            }
        }

        scoreLabel.setText("Điểm: " + score);
        nextButton.setDisable(false); // Kích hoạt nút Next
    }

    @FXML
    void handleNextQuestion(ActionEvent event) {
        currentQuestionIndex++;
        loadQuestion();
    }

    @FXML
    void handleRestart(ActionEvent event) {
        // Reset trạng thái
        score = 0;
        currentQuestionIndex = 0;
        selectedButton = null;
        scoreLabel.setText("Điểm: 0");
        Collections.shuffle(questions); // Xáo trộn lại câu hỏi

        // Hiển thị lại phần quiz, ẩn phần kết quả
        resultsPane.setVisible(false);
        resultsPane.setManaged(false);
        quizPane.setVisible(true);
        quizPane.setManaged(true);

        loadQuestion(); // Tải câu hỏi đầu tiên
    }


    private void showResults() {
        quizPane.setVisible(false); // Ẩn phần câu hỏi
        quizPane.setManaged(false); // Không quản lý layout cho phần câu hỏi
        resultsPane.setVisible(true); // Hiện phần kết quả
        resultsPane.setManaged(true); // Quản lý layout cho phần kết quả
        finalScoreLabel.setText("Điểm cuối cùng của bạn: " + score + " / " + questions.size());
    }

    private void resetButtonsStyle() {
        answerButton1.getStyleClass().removeAll("correct-answer", "incorrect-answer");
        answerButton2.getStyleClass().removeAll("correct-answer", "incorrect-answer");
        answerButton3.getStyleClass().removeAll("correct-answer", "incorrect-answer");
        answerButton4.getStyleClass().removeAll("correct-answer", "incorrect-answer");
    }

    private void enableAnswerButtons(boolean enable) {
        answerButton1.setDisable(!enable);
        answerButton2.setDisable(!enable);
        answerButton3.setDisable(!enable);
        answerButton4.setDisable(!enable);
    }

    // Helper để lấy nút Button dựa trên index (0-3)
    private Button getButtonByIndex(int index) {
        switch (index) {
            case 0: return answerButton1;
            case 1: return answerButton2;
            case 2: return answerButton3;
            case 3: return answerButton4;
            default: return null;
        }
    }
}