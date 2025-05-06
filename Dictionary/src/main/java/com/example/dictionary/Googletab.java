package com.example.dictionary;

import Function.ChangeStage;
import com.example.dictionary.googletab.TranslationService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import javafx.animation.KeyFrame;  // Import KeyFrame
import javafx.animation.Timeline; // Import Timeline
import javafx.util.Duration;      // Import Duration


import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.ExecutorService; // Không cần ExecutorService trong Controller nữa
// import java.util.concurrent.Executors; // Không cần Executors trong Controller nữa


public class Googletab extends ChangeStage implements Initializable {

    // --- Thành phần UI (View) ---
    @FXML
    private TextArea englishTextArea;
    @FXML
    private TextArea vietnameseTextArea;
    @FXML
    private Button translateButton; // Giữ nút này để dịch ngay lập tức nếu cần
    @FXML
    private Button backButton;

    // --- Đối tượng chức năng (Service) mới ---
    private TranslationService translationService;

    // --- Debounce Timeline cho tính năng dịch live ---
    private Timeline debounceTimeline;
    private final Duration DEBOUNCE_DELAY = Duration.millis(400); // Độ trễ (ví dụ: 400ms) sau khi ngừng gõ

    // Constructor mặc định được sử dụng bởi FXMLLoader.
    public Googletab() {
        // Khởi tạo Service ở đây
        this.translationService = new TranslationService();
    }

    // --- Phương thức initialize (Thiết lập ban đầu) ---
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupInitialUIState();  // Cấu hình trạng thái UI ban đầu
        setupButtonActions();   // Gắn action cho các nút
        setupTextListener();    // Gắn listener cho text area
        System.out.println("Translation Controller Initialized.");
    }

    // --- Phương thức Setup Helper ---

    // setupServices() không còn cần thiết nếu khởi tạo trong constructor

    private void setupInitialUIState() {
        englishTextArea.setPromptText("Enter English text here...");
        vietnameseTextArea.setPromptText("Translation will appear here...");
        vietnameseTextArea.setEditable(false); // Vùng kết quả không nên chỉnh sửa
        vietnameseTextArea.setStyle("-fx-text-fill: black;"); // Màu mặc định
    }

    private void setupButtonActions() {
        // Nút dịch giờ sẽ ép buộc dịch ngay lập tức
        translateButton.setOnAction(event -> handleTranslateButtonAction());
        backButton.setOnAction(event -> handleBack(event));
    }

    // --- Thiết lập Listener cho TextArea để dịch live ---
    private void setupTextListener() {
        englishTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            handleTextInputChange(newValue);
        });
    }

    // --- Logic xử lý sự kiện UI (@FXML Methods) ---

    @FXML
    void handleBack(ActionEvent event) {
        changeStage(backButton, "main.fxml", getClass());
    }

    // Xử lý khi nút Dịch được nhấn (buộc dịch ngay lập tức)
    @FXML
    protected void handleTranslateButtonAction() {
        // Dừng debounce timer nếu nó đang chạy, để tránh dịch 2 lần
        if (debounceTimeline != null) {
            debounceTimeline.stop();
        }
        // Bắt đầu dịch ngay lập tức
        startTranslationProcess();
    }

    // --- Logic xử lý thay đổi văn bản (cho dịch live) ---
    private void handleTextInputChange(String newText) {
        // Dừng bộ đếm thời gian debounce hiện tại
        if (debounceTimeline != null) {
            debounceTimeline.stop();
        }

        // Nếu văn bản trống, xóa kết quả và không làm gì thêm
        if (newText == null || newText.isBlank()) {
            vietnameseTextArea.setText("");
            vietnameseTextArea.setStyle("-fx-text-fill: black;"); // Đặt lại màu chữ bình thường
            // Không cần bắt đầu dịch nếu input trống
            return;
        }

        // Khởi tạo Timeline mới cho debounce
        debounceTimeline = new Timeline(
                new KeyFrame(DEBOUNCE_DELAY, event -> startTranslationProcess()) // Sau độ trễ, gọi startTranslationProcess
        );

        // Bắt đầu bộ đếm thời gian
        debounceTimeline.playFromStart();

        // Cập nhật UI nhẹ nhàng khi người dùng bắt đầu gõ sau khi có kết quả cũ
        // Không hiển thị "Translating..." ngay lập tức, chỉ hiển thị khi debounce xong
        if (!vietnameseTextArea.getText().isEmpty() && !vietnameseTextArea.getText().equals("Translating...")) {
            vietnameseTextArea.setText("..."); // Có thể hiển thị dấu ... hoặc xóa hẳn
            vietnameseTextArea.setStyle("-fx-text-fill: gray;"); // Đổi màu chữ tạm thời
        }
    }


    // --- Phương thức bắt đầu quá trình dịch (được gọi bởi debounce hoặc nút) ---
    private void startTranslationProcess() {
        String textToTranslate = englishTextArea.getText();

        // Kiểm tra lại nếu text trống (trường hợp người dùng xóa hết text trong lúc debounce)
        if (textToTranslate == null || textToTranslate.isBlank()) {
            vietnameseTextArea.setText("");
            vietnameseTextArea.setStyle("-fx-text-fill: black;");
            return;
        }

        // 1. Cập nhật UI khi bắt đầu dịch thực sự (sau debounce)
        updateUIForTranslationStart();

        // 2. Gọi phương thức dịch từ Service (trả về CompletableFuture)
        CompletableFuture<String> translationFuture = translationService.translate(textToTranslate, "en", "vi");

        // 3. Xử lý kết quả trên luồng JavaFX Application Thread
        translationFuture
                .thenAcceptAsync(translatedText -> {
                    // Cần kiểm tra xem text input có bị thay đổi trong lúc chờ dịch không
                    // Nếu có, kết quả này đã cũ, không cập nhật UI
                    if (englishTextArea.getText().equals(textToTranslate)) { // So sánh với text lúc bắt đầu yêu cầu này
                        handleTranslationSuccess(translatedText);
                    } else {
                        System.out.println("Ignored old translation result for: " + textToTranslate);
                        // Nếu kết quả cũ, UI sẽ được cập nhật bởi yêu cầu dịch mới nhất
                        // Không cần làm gì ở đây, chỉ log để biết
                    }
                }, Platform::runLater)
                // 4. Xử lý lỗi trên luồng JavaFX Application Thread
                .exceptionally(error -> {
                    // Cần kiểm tra xem text input có bị thay đổi trong lúc chờ dịch không
                    // Nếu có, lỗi này có thể không liên quan đến input hiện tại
                    if (englishTextArea.getText().equals(textToTranslate)) { // So sánh với text lúc bắt đầu yêu cầu này
                        handleTranslationErrorAndRecovery(error);
                    } else {
                        System.out.println("Ignored old translation error for: " + textToTranslate);
                        // Tương tự, chỉ log
                    }
                    return null; // Cần return null cho exceptionally
                });
    }


    // --- Các phương thức Helper (Private Methods) ---
    // Các phương thức này chịu trách nhiệm CẬP NHẬT UI

    // Helper để cập nhật UI khi bắt đầu quá trình dịch thực sự (sau debounce)
    private void updateUIForTranslationStart() {
        // Không disable nút dịch nữa khi gõ
        // translateButton.setDisable(true);
        vietnameseTextArea.setText("Translating...");
        vietnameseTextArea.setStyle("-fx-text-fill: gray;"); // Màu xám khi đang dịch
    }

    // Helper để xử lý kết quả dịch thành công và cập nhật UI trên luồng JavaFX
    private Void handleTranslationSuccess(String translatedText) {
        vietnameseTextArea.setText(translatedText);
        // Không enable nút dịch nữa, nó luôn enabled
        // translateButton.setDisable(false);
        vietnameseTextArea.setStyle("-fx-text-fill: black;"); // Đặt lại màu chữ bình thường
        return null; // Cần return null cho exceptionally
    }

    // Helper để xử lý lỗi dịch, cập nhật UI và khôi phục trạng thái trên luồng JavaFX
    private Void handleTranslationErrorAndRecovery(Throwable error) {
        // Log chi tiết lỗi ra console để debug
        System.err.println("Translation Error Occurred:");
        Throwable cause = error.getCause() != null ? error.getCause() : error; // Lấy nguyên nhân gốc
        cause.printStackTrace();

        // Hiển thị thông báo lỗi thân thiện hơn cho người dùng
        String errorMessage = "Error: Could not translate.\nDetails: ";
        // Rút gọn thông báo lỗi hiển thị cho người dùng
        if (cause instanceof IOException) {
            errorMessage += "Network or connection issue.";
        } else if (cause instanceof InterruptedException) {
            errorMessage += "Translation interrupted.";
        } else if (cause instanceof IllegalArgumentException) {
            errorMessage += "Invalid input character or URL issue.";
        } else if (cause.getMessage() != null && !cause.getMessage().trim().isEmpty()) {
            String detail = cause.getMessage();
            // Giới hạn độ dài chi tiết lỗi hiển thị trên UI
            if (detail.length() > 150) detail = detail.substring(0, 150) + "...";
            errorMessage += detail;
        } else {
            errorMessage += "Unknown error.";
        }

        vietnameseTextArea.setText(errorMessage);
        // Không enable nút dịch nữa, nó luôn enabled
        // translateButton.setDisable(false);
        vietnameseTextArea.setStyle("-fx-text-fill: red;"); // Đổi màu chữ báo lỗi
        return null; // Cần return null cho exceptionally
    }
}