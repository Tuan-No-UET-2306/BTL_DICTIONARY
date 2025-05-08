package com.example.dictionary; // Đảm bảo đúng package

import Function.ChangeStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.application.Platform; // Import Platform for runLater

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
// import java.util.Collections; // Nếu bạn muốn xáo trộn câu hỏi

public class QuizController implements Initializable {

    // KHÔNG CÒN DÙNG RECORD NỮA
    // private record Question(String questionText, String[] options, String correctAnswer) {}

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
    private List<Question> questions; // Sử dụng lớp Question mới

    // Manager cho VBox thông báo
    private ResultNotificationManager resultNotificationManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Quiz Controller Initialized.");

        if (rootPane == null) {
            System.err.println("Lỗi: rootPane chưa được inject! Kiểm tra fx:id trong FXML.");
            return;
        }

        resultNotificationManager = new ResultNotificationManager(
                rootPane,
                this::proceedToNextQuestionFromNotification,
                this::endQuizFromNotification
        );

        initializeQuestions(); // Khởi tạo danh sách câu hỏi

        if (questions != null && !questions.isEmpty()) {
            loadQuestion(currentQuestionIndex);
        } else {
            if(questionLabel != null) questionLabel.setText("Không có câu hỏi nào.");
            if(answersGrid != null) answersGrid.setVisible(false);
            if(nextButton != null) nextButton.setDisable(true);
            if(backButton != null) backButton.setDisable(true);
        }

        if(nextButton != null) nextButton.setTooltip(new Tooltip("Go to the next question"));
        if(backButton != null) backButton.setTooltip(new Tooltip("Go to the previous question"));

        // Thử nghiệm chuyển focus để giải quyết vấn đề notification tự hiện (nếu có)
        // Bạn có thể bật/tắt dòng này để kiểm tra
        if (rootPane != null) {
            Platform.runLater(() -> {
                rootPane.requestFocus();
                System.out.println("DEBUG: Focus requested on rootPane in initialize.");
            });
        }
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

    @FXML
    void handleBackAction(ActionEvent event) {
        if (resultNotificationManager != null) resultNotificationManager.hide();

        if (currentQuestionIndex > 0) {
            System.out.println("Back button clicked! Going to previous question.");
            currentQuestionIndex--;
            loadQuestion(currentQuestionIndex);
        } else {
            System.out.println("Back button clicked, but already at the first question.");
        }
    }
    //đầu tiên ta khởi tạo biến currentQuestionIndex để đếm số câu hỏi (tránh mức tràn bộ nhớ cho phép)
    //tiếp theo nút back action sẽ có tác dụng cập nhật câu hỏi trước đó bằng phương thức loadquestion

    @FXML
    void handleNextAction(ActionEvent event) {
        if (resultNotificationManager != null) resultNotificationManager.hide();
        System.out.println("Next button (main) clicked!");
        if (currentQuestionIndex + 1 < questions.size()) {
            currentQuestionIndex++;
            loadQuestion(currentQuestionIndex);
        } else {
            System.out.println("End of quiz reached by main next button.");
            showEndOfQuiz();
        }
    }
    //tương tự nut next co chức năng tương tự và gọi phương thức load question để update câu hỏi trường hợp currentQuestionindex số lượng thì game quizz kết thúc

    private void proceedToNextQuestionFromNotification() {
        System.out.println("Proceeding to next question from notification.");
        // resultNotificationManager.hide() đã được gọi bên trong manager rồi
        if (currentQuestionIndex + 1 < questions.size()) {
            handleNextAction(null); // Gọi handleNextAction chung để xử lý
        } else {
            System.err.println("Error: proceedToNextQuestionFromNotification called on last question. Should call endQuiz.");
            showEndOfQuiz(); // Xử lý như kết thúc quiz
        }
    }

    private void endQuizFromNotification() {
        System.out.println("Ending quiz from notification.");
        // resultNotificationManager.hide() đã được gọi bên trong manager rồi
        showEndOfQuiz();
    }


    // ... (trong QuizController.java)

    @FXML
    void handleAnswerAction(ActionEvent event) {
        // ... (các dòng debug và kiểm tra ban đầu giữ nguyên) ...

        if (resultNotificationManager != null && resultNotificationManager.isShowing()) {
            // ...
            return;
        }
        if (questions == null || questions.isEmpty() || currentQuestionIndex >= questions.size()) {
            // ...
            return;
        }

        Button clickedButton = null;
        if (event != null && event.getSource() instanceof Button) {
            clickedButton = (Button) event.getSource();
        } else {
            // ...
            return;
        }

        String chosenAnswer = clickedButton.getText();
        // ...

        Question currentQ = questions.get(currentQuestionIndex);

        // === THAY ĐỔI CHÍNH ===
        // Gọi trực tiếp phương thức isCorrect() từ đối tượng Question
        boolean isCorrect = currentQ.isCorrect(chosenAnswer);
        // =====================

        disableInteraction();
        boolean isLastQuestion = (currentQuestionIndex + 1 >= questions.size());
        // ...
        if (resultNotificationManager != null) {
            resultNotificationManager.show(isCorrect, currentQ.getCorrectAnswer(), isLastQuestion);
        } else {
            // ...
        }
    }

    // === PHƯƠNG THỨC checkAnswer ĐÃ BỊ XÓA KHỎI QuizController ===
    // private boolean checkAnswer(Question question, String chosenAnswer) {
    //     // ...
    // }
    // ==============================================================

// ... (phần còn lại của QuizController giữ nguyên)

    private void disableInteraction() {
        setNodesDisabled(true, answersGrid, nextButton, backButton);
    }
    //vô hiệu hóa làm mờ các nút chính

    private void enableInteraction() {
        setNodesDisabled(false, answersGrid);
        if (backButton != null) {
            backButton.setDisable(currentQuestionIndex <= 0);
        }
        // kích hoạt lại làm cho các nút được tương tác trừ trường hợp câu hỏi đầu tiên
        if (nextButton != null) {
            boolean isEffectivelyLastQuestion = (questions == null || questions.isEmpty() || currentQuestionIndex >= questions.size() - 1);
            nextButton.setDisable(isEffectivelyLastQuestion);
        }
    }

    private void setNodesDisabled(boolean disabled, Node... nodes) {
        for (Node node : nodes) {
            if (node != null) {
                if (node instanceof GridPane) {
                    ((GridPane) node).getChildren().forEach(child -> child.setDisable(disabled));
                } else {
                    node.setDisable(disabled);
                }
            }
        }
    }

    private void loadQuestion(int questionIndex) {
        System.out.println("DEBUG: loadQuestion called for index: " + questionIndex);

        // 1. Kiểm tra đầu vào cơ bản (danh sách câu hỏi và chỉ số)
        //    Nếu không hợp lệ, thiết lập trạng thái lỗi và thoát.
        if (questions == null || questions.isEmpty() || questionIndex < 0 || questionIndex >= questions.size()) {
            System.err.println("Invalid question index or no questions: " + questionIndex);
            // Thiết lập UI cho trạng thái lỗi/không có câu hỏi
            if (questionLabel != null) questionLabel.setText("Không thể tải câu hỏi.");
            if (answersGrid != null) answersGrid.setVisible(false); // Ẩn khu vực trả lời
            // Vô hiệu hóa các nút điều hướng chính
            if (nextButton != null) nextButton.setDisable(true);
            if (backButton != null) backButton.setDisable(true);
            return; // Thoát sớm
        }

        // 2. Lấy câu hỏi hiện tại (đã qua kiểm tra ở bước 1)
        Question currentQ = questions.get(questionIndex);

        // 3. Hiển thị nội dung câu hỏi và đảm bảo khu vực trả lời được hiển thị
        if (questionLabel != null) {
            questionLabel.setText(currentQ.getQuestionText());
        }
        if (answersGrid != null) {
            answersGrid.setVisible(true); // Đảm bảo lưới đáp án hiển thị
            // Kích hoạt lại các nút trong lưới (sẽ được enableInteraction() xử lý chi tiết hơn)
            // setNodesDisabled(false, answersGrid); // Có thể gọi trực tiếp hoặc để enableInteraction xử lý
        }

        // 4. Lấy và hiển thị các lựa chọn lên các nút trả lời
        //    Giả định: currentQ.getOptions() luôn trả về mảng String[4] hợp lệ
        //    do constructor của Question đã đảm bảo điều này.
        String[] options = currentQ.getOptions();

        // Gán trực tiếp, vì ta tin tưởng Question constructor đã validate options
        // Các kiểm tra (button != null) vẫn cần thiết phòng trường hợp FXML chưa inject đúng
        if (answerButton1 != null) answerButton1.setText(options[0]);
        if (answerButton2 != null) answerButton2.setText(options[1]);
        if (answerButton3 != null) answerButton3.setText(options[2]);
        if (answerButton4 != null) answerButton4.setText(options[3]);

        // Nếu giả định về Question constructor không đúng, và options có thể không hợp lệ ở đây,
        // thì khối if/else kiểm tra options.length == 4 là cần thiết.
        // Tuy nhiên, để "rút gọn" và theo thiết kế tốt, Question nên tự đảm bảo tính hợp lệ của nó.

        // 5. Kích hoạt lại các tương tác và log
        enableInteraction(); // Quan trọng: Kích hoạt lại các nút trả lời và điều chỉnh nút Next/Back
        System.out.println("Đã tải câu hỏi " + (questionIndex + 1) + "/" + questions.size() + ": " + currentQ.getQuestionText());
    }


    private void showEndOfQuiz() {
        System.out.println("Quiz đã kết thúc!");
        if (resultNotificationManager != null) resultNotificationManager.hide();

        if(questionLabel != null) questionLabel.setText("Chúc mừng bạn đã hoàn thành Quiz!");
        if(answersGrid != null) answersGrid.setVisible(false);
        if(nextButton != null) nextButton.setDisable(true);
        if(backButton != null) {
            // Chỉ bật nút back nếu không phải là câu hỏi đầu tiên hoặc không có câu hỏi nào
            backButton.setDisable(questions == null || questions.isEmpty() || currentQuestionIndex <= 0);
        }
    }

    @FXML // Đảm bảo @FXML được thêm nếu phương thức này được gọi từ FXML (mặc dù trong code hiện tại nó được gọi từ code)
    public void handleExit(ActionEvent event) { // Thêm @FXML nếu cần
        // Có thể không cần setOnAction ở đây nếu đã set trong FXML
        // exitQuizz.setOnAction(e -> {
        //     ChangeStage.changeStage(exitQuizz, "game_tab.fxml", getClass());
        // });
        // Nếu onAction đã được đặt trong FXML, dòng sau là đủ:
        ChangeStage.changeStage(exitQuizz, "game_tab.fxml", getClass());
    }
}