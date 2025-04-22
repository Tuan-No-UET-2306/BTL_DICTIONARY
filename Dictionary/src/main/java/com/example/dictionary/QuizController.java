package com.example.dictionary; // Đảm bảo đúng package

import Function.ChangeStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
// Bỏ import FXMLLoader, Parent, Scene, Stage nếu không dùng chức năng quay về main.fxml nữa
// import javafx.fxml.FXMLLoader;
// import javafx.scene.Parent;
// import javafx.scene.Scene;
// import javafx.stage.Stage;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.Node;

// Bỏ import IOException nếu không dùng chức năng quay về main.fxml nữa
// import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class QuizController implements Initializable {

    // Lớp nội bộ hoặc record để biểu diễn câu hỏi
    private record Question(String questionText, String[] options, String correctAnswer) {}

    // Các thành phần FXML
    @FXML private AnchorPane rootPane;
    @FXML private Button backButton;
    @FXML private Label titleLabel;
    @FXML private Button nextButton; // Nút next chính
    @FXML private Region questionAreaRegion;
    @FXML private Label questionLabel;
    @FXML private GridPane answersGrid;
    @FXML private Button answerButton1;
    @FXML private Button answerButton2;
    @FXML private Button answerButton3;
    @FXML private Button answerButton4;
    @FXML private Button exitQuizz;

    // Trạng thái Quiz
    private int currentQuestionIndex = 0;
    private List<Question> questions;

    // Lưu trữ VBox thông báo được tạo bằng code
    private VBox resultNotificationVBox = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Quiz Controller Initialized.");
        initializeQuestions(); // Khởi tạo danh sách câu hỏi

        if (rootPane == null) {
            System.err.println("Lỗi: rootPane chưa được inject! Kiểm tra fx:id trong FXML.");
            return;
        }

        if (questions != null && !questions.isEmpty()) {
            loadQuestion(currentQuestionIndex);
        } else {
            // Xử lý khi không có câu hỏi
            if(questionLabel != null) questionLabel.setText("Không có câu hỏi nào.");
            if(answersGrid != null) answersGrid.setVisible(false);
            if(nextButton != null) nextButton.setDisable(true);
            if(backButton != null) backButton.setDisable(true); // Disable cả back nếu không có câu hỏi
        }

        // Gán Tooltip cho các nút điều hướng
        if(nextButton != null) nextButton.setTooltip(new Tooltip("Go to the next question"));
        if(backButton != null) backButton.setTooltip(new Tooltip("Go to the previous question")); // Sửa tooltip
    }

    // Phương thức khởi tạo danh sách câu hỏi
    private void initializeQuestions() {
        questions = new ArrayList<>();
        // --- Thêm các câu hỏi của bạn vào đây ---
        questions.add(new Question("Which word is a synonym for 'big'?", new String[]{"Small", "Large", "Tiny", "Quiet"}, "Large"));
        questions.add(new Question("What is the opposite of 'cold'?", new String[]{"Warm", "Cool", "Freezing", "Hot"}, "Hot"));
        questions.add(new Question("What does 'ancient' mean?", new String[]{"New", "Modern", "Very old", "Fast"}, "Very old"));
        questions.add(new Question("Find the synonym for 'fast'.", new String[]{"Slow", "Quick", "Easy", "Heavy"}, "Quick"));
        questions.add(new Question("The antonym of 'sad' is...", new String[]{"Unhappy", "Angry", "Excited", "Happy"}, "Happy"));
        questions.add(new Question("Which word means the same as 'beautiful'?", new String[]{"Ugly", "Pretty", "Simple", "Loud"}, "Pretty"));
        questions.add(new Question("What is the opposite of 'up'?", new String[]{"Over", "Under", "Down", "Across"}, "Down"));
        questions.add(new Question("A 'doctor' works in a...", new String[]{"School", "Restaurant", "Hospital", "Library"}, "Hospital"));
        questions.add(new Question("Which word is a synonym for 'clever'?", new String[]{"Stupid", "Dull", "Smart", "Weak"}, "Smart"));
        // --- Thêm bao nhiêu câu hỏi tùy ý ---
        // Collections.shuffle(questions); // Tùy chọn: Xáo trộn câu hỏi
    }

    // --- Xử lý sự kiện cho nút Back (Câu hỏi trước) ---
    @FXML
    void handleBackAction(ActionEvent event) {
        removeResultNotification(); // Xóa VBox thông báo nếu đang hiển thị

        // Chỉ quay lại câu trước nếu không phải đang ở câu đầu tiên (index > 0)
        if (currentQuestionIndex > 0) {
            System.out.println("Back button clicked! Going to previous question.");
            currentQuestionIndex--; // Giảm chỉ số câu hỏi đi 1
            loadQuestion(currentQuestionIndex); // Tải lại câu hỏi ở chỉ số mới
        } else {
            System.out.println("Back button clicked, but already at the first question.");
            // Không làm gì khi ở câu đầu tiên
        }
    }

    // --- Xử lý sự kiện cho nút Next chính ---
    @FXML
    void handleNextAction(ActionEvent event) {
        removeResultNotification();
        System.out.println("Next button clicked!");
        if (currentQuestionIndex + 1 < questions.size()) { // Kiểm tra nếu còn câu hỏi tiếp theo
            currentQuestionIndex++;
            loadQuestion(currentQuestionIndex);
        } else {
            System.out.println("End of quiz reached.");
            showEndOfQuiz(); // Hiển thị trạng thái kết thúc quiz
        }
    }

    // --- Xử lý sự kiện khi người dùng chọn một đáp án ---
    @FXML
    void handleAnswerAction(ActionEvent event) {
        // Không xử lý nếu VBox thông báo đang hiển thị
        if (resultNotificationVBox != null) {
            return;
        }
        // Không xử lý nếu quiz đã kết thúc
        if (currentQuestionIndex >= questions.size()) {
            System.out.println("Quiz already finished. Cannot answer.");
            return;
        }

        Button clickedButton = (Button) event.getSource();
        String chosenAnswer = clickedButton.getText();
        System.out.println("Answer chosen: " + chosenAnswer);

        Question currentQ = questions.get(currentQuestionIndex); // Lấy câu hỏi hiện tại
        boolean isCorrect = checkAnswer(currentQ, chosenAnswer); // Kiểm tra đáp án

        disableInteraction(); // Vô hiệu hóa các nút
        showResultNotification(isCorrect, currentQ.correctAnswer()); // Hiển thị VBox thông báo
    }

    // --- Hàm tạo VBox thông báo bằng code và hiển thị ---
    private void showResultNotification(boolean isCorrect, String correctAnswer) {
        resultNotificationVBox = new VBox(15);
        resultNotificationVBox.setAlignment(Pos.CENTER);
        resultNotificationVBox.setPadding(new Insets(25, 30, 25, 30));

        Label titleMsgLabel = new Label();
        Label detailsMsgLabel = new Label();
        Button notificationNextButton = new Button("NEXT");

        // Đặt nội dung và gán Style Class dựa trên kết quả
        if (isCorrect) {
            titleMsgLabel.setText("CHÚC MỪNG!");
            detailsMsgLabel.setText("Bạn đã trả lời đúng!\nĐáp án chính xác là: " + correctAnswer);
            resultNotificationVBox.getStyleClass().add("result-vbox-correct");
        } else {
            titleMsgLabel.setText("RẤT TIẾC!");
            detailsMsgLabel.setText("Bạn đã trả lời sai.\nĐáp án đúng là: " + correctAnswer);
            resultNotificationVBox.getStyleClass().add("result-vbox-incorrect");
        }
        // Gán các class chung để CSS áp dụng
        resultNotificationVBox.getStyleClass().add("result-vbox");
        titleMsgLabel.getStyleClass().add("result-title");
        detailsMsgLabel.getStyleClass().add("result-details");
        notificationNextButton.getStyleClass().add("result-next-button");

        // Gắn sự kiện cho nút NEXT trong VBox
        notificationNextButton.setOnAction(e -> {
            removeResultNotification(); // Xóa VBox
            if (currentQuestionIndex + 1 < questions.size()) {
                handleNextAction(null); // Chuyển câu hỏi tiếp theo
            } else {
                showEndOfQuiz(); // Kết thúc quiz nếu đây là câu cuối
            }
        });

        resultNotificationVBox.getChildren().addAll(titleMsgLabel, detailsMsgLabel, notificationNextButton);

        // Thêm VBox vào rootPane và định vị
        if (rootPane != null) {
            rootPane.getChildren().add(resultNotificationVBox);
            AnchorPane.setTopAnchor(resultNotificationVBox, 150.0);
            AnchorPane.setLeftAnchor(resultNotificationVBox, 200.0);
            AnchorPane.setRightAnchor(resultNotificationVBox, 200.0);
        } else {
            System.err.println("Không thể thêm VBox thông báo vì rootPane là null!");
            resultNotificationVBox = null; // Reset nếu lỗi
            enableInteraction(); // Cho phép tương tác lại nếu không hiện được VBox
        }
    }

    // --- Hàm xóa VBox thông báo ---
    private void removeResultNotification() {
        if (resultNotificationVBox != null && rootPane != null) {
            rootPane.getChildren().remove(resultNotificationVBox);
            resultNotificationVBox = null; // Reset biến tham chiếu
        }
    }

    // --- Các hàm tiện ích để quản lý trạng thái nút ---
    private void disableInteraction() {
        setNodesDisabled(true, answersGrid, nextButton, backButton);
    }

    private void enableInteraction() {
        setNodesDisabled(false, answersGrid); // Bật lại các nút trả lời
        if (backButton != null) {
            backButton.setDisable(currentQuestionIndex <= 0); // Chỉ disable back ở câu đầu
        }
        if (nextButton != null) {
            // Disable nút next chính nếu đang ở câu cuối
            boolean isLastQuestion = (currentQuestionIndex >= questions.size() - 1);
            nextButton.setDisable(isLastQuestion);
        }
    }

    // Hàm tiện ích để bật/tắt nhiều node (bao gồm cả các nút trong GridPane)
    private void setNodesDisabled(boolean disabled, Node... nodes) {
        for (Node node : nodes) {
            if (node != null) {
                if (node instanceof GridPane) {
                    // Vô hiệu hóa/Kích hoạt tất cả các nút con trong GridPane
                    ((GridPane) node).getChildren().forEach(child -> child.setDisable(disabled));
                } else {
                    node.setDisable(disabled); // Áp dụng cho các nút khác
                }
            }
        }
    }

    // --- Hàm tải dữ liệu câu hỏi lên giao diện ---
    private void loadQuestion(int questionIndex) {
        // Kiểm tra index hợp lệ
        if (questionIndex < 0 || questionIndex >= questions.size()) {
            System.err.println("Index câu hỏi không hợp lệ: " + questionIndex);
            // Không nên gọi showEndOfQuiz ở đây vì có thể người dùng bấm back từ câu 0
            // Chỉ cần đảm bảo không làm gì nếu index sai
            return;
        }

        Question currentQ = questions.get(questionIndex); // Lấy câu hỏi

        // Cập nhật các thành phần UI (kiểm tra null trước)
        if(questionLabel != null) questionLabel.setText(currentQ.questionText());
        if(answersGrid != null) answersGrid.setVisible(true); // Đảm bảo grid hiển thị

        // Gán text cho các nút trả lời (kiểm tra mảng options)
        if (currentQ.options() != null && currentQ.options().length == 4) {
            if(answerButton1 != null) answerButton1.setText(currentQ.options()[0]);
            if(answerButton2 != null) answerButton2.setText(currentQ.options()[1]);
            if(answerButton3 != null) answerButton3.setText(currentQ.options()[2]);
            if(answerButton4 != null) answerButton4.setText(currentQ.options()[3]);
        } else {
            System.err.println("Lỗi: Câu hỏi " + questionIndex + " không đủ 4 lựa chọn.");
            // Xử lý lỗi hiển thị (ví dụ: đặt text "Lỗi" cho nút)
            if(answerButton1 != null) answerButton1.setText("Lỗi");
            if(answerButton2 != null) answerButton2.setText("Lỗi");
            if(answerButton3 != null) answerButton3.setText("Lỗi");
            if(answerButton4 != null) answerButton4.setText("Lỗi");
        }

        enableInteraction(); // Kích hoạt/Vô hiệu hóa các nút điều hướng cho phù hợp
        System.out.println("Đã tải câu hỏi " + (questionIndex + 1) + "/" + questions.size());
    }

    // --- Hàm kiểm tra đáp án ---
    private boolean checkAnswer(Question question, String chosenAnswer) {
        // So sánh text người dùng chọn với đáp án đúng lưu trong Question
        return chosenAnswer.equals(question.correctAnswer());
    }

    // --- Hàm xử lý khi kết thúc Quiz ---
    private void showEndOfQuiz() {
        System.out.println("Quiz đã kết thúc!");
        // Cập nhật UI để báo quiz đã xong
        if(questionLabel != null) questionLabel.setText("Chúc mừng bạn đã hoàn thành Quiz!");
        if(answersGrid != null) answersGrid.setVisible(false); // Ẩn các nút trả lời
        if(nextButton != null) nextButton.setDisable(true); // Vô hiệu hóa nút next chính
        if(backButton != null) backButton.setDisable(false); // Giữ nút back bật để có thể xem lại câu cuối

        // TODO: Thêm logic hiển thị điểm số cuối cùng hoặc nút "Chơi lại"
    }

    public void handleExit(ActionEvent event) {
        exitQuizz.setOnAction(e -> {
            ChangeStage.changeStage(exitQuizz, "game_tab.fxml", getClass());
        });
    }
}