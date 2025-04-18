package com.example.dictionary;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.util.*;

public class WordleController implements Initializable {

    // --- Các hằng số và biến trạng thái game (giống như trước) ---
    private static final int MAX_GUESSES = 6;
    private static final int WORD_LENGTH = 5;
    private static final String[] WORD_LIST = {
            "CHUOI", "QUYEN", "PHUOC", "MAY mắn", "TRUOT",
            "APPLE", "TABLE", "CHAIR", "MOUSE", "HOUSE", "TRACE", "CRANE"
    };

    private String secretWord;
    private int currentGuess = 0;
    private Label[][] letterCells = new Label[MAX_GUESSES][WORD_LENGTH];

    private enum Result { CORRECT, WRONG_POSITION, INCORRECT }

    // --- Tham chiếu đến các thành phần UI từ FXML ---
    // Tên biến phải trùng với fx:id trong file FXML
    @FXML
    private GridPane wordleGrid;

    @FXML
    private TextField guessInput;

    @FXML
    private Button guessButton;

    @FXML
    private Label messageLabel;

    @FXML
    private Button restartButton;

    /**
     * Phương thức này được gọi tự động sau khi file FXML được load.
     * Đây là nơi tốt nhất để khởi tạo game ban đầu.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupGrid(); // Tạo các ô Label ban đầu cho GridPane
        initializeGame();
    }

    /**
     * Tạo các ô Label và thêm vào GridPane lần đầu.
     */
    private void setupGrid() {
        wordleGrid.getChildren().clear(); // Xóa mọi thứ cũ (quan trọng khi chơi lại)
        for (int row = 0; row < MAX_GUESSES; row++) {
            for (int col = 0; col < WORD_LENGTH; col++) {
                Label cell = createLetterCell();
                letterCells[row][col] = cell;
                // Quan trọng: Thêm cell vào GridPane tại đúng vị trí cột, hàng
                wordleGrid.add(cell, col, row);
            }
        }
        // Căn giữa GridPane (nếu cần)
        wordleGrid.setAlignment(Pos.CENTER);
    }

    /**
     * Hàm tiện ích tạo một ô Label (giống như trước)
     */
    private Label createLetterCell() {
        Label cell = new Label("");
        cell.setPrefSize(50, 50);
        cell.setAlignment(Pos.CENTER);
        cell.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        cell.setTextFill(Color.WHITE);
        cell.setStyle("-fx-background-color: #bdc3c7; -fx-border-color: #7f8c8d; -fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3;");
        return cell;
    }


    /**
     * Reset game về trạng thái ban đầu (gần giống trước, nhưng cập nhật UI qua @FXML)
     */
    private void initializeGame() {
        Random random = new Random();
        secretWord = WORD_LIST[random.nextInt(WORD_LIST.length)].toUpperCase();
        System.out.println("Từ bí mật (để test): " + secretWord);

        currentGuess = 0;

        // Reset giao diện lưới sử dụng mảng letterCells đã được tạo trong setupGrid
        for (int row = 0; row < MAX_GUESSES; row++) {
            for (int col = 0; col < WORD_LENGTH; col++) {
                Label cell = letterCells[row][col];
                cell.setText("");
                cell.setStyle("-fx-background-color: #bdc3c7; -fx-border-color: #7f8c8d; -fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3;");
                cell.setTextFill(Color.WHITE);
            }
        }

        messageLabel.setText("Bắt đầu đoán!");
        messageLabel.setTextFill(Color.BLACK);
        guessInput.clear();
        guessInput.setDisable(false);
        guessButton.setDisable(false);
        restartButton.setVisible(false);
        guessInput.requestFocus();
    }

    /**
     * Xử lý sự kiện khi nhấn nút "Đoán" hoặc Enter trong TextField.
     * Tên phương thức này phải trùng với giá trị onAction trong FXML.
     */
    @FXML
    private void handleGuess() {
        String guess = guessInput.getText().toUpperCase().trim();

        if (guess.length() != WORD_LENGTH) {
            setMessage("Từ phải có đúng " + WORD_LENGTH + " ký tự!", Color.RED);
            return;
        }
        // Optional: Kiểm tra từ hợp lệ

        Result[] feedback = processGuess(guess);
        updateGrid(guess, feedback);

        if (guess.equals(secretWord)) {
            setMessage("Chúc mừng! Bạn đã đoán đúng!", Color.GREEN);
            endGame(true);
        } else {
            currentGuess++;
            if (currentGuess >= MAX_GUESSES) {
                setMessage("Rất tiếc! Bạn đã hết lượt. Từ bí mật là: " + secretWord, Color.RED);
                endGame(false);
            } else {
                setMessage("Tiếp tục đoán nào!", Color.BLACK);
                guessInput.clear();
            }
        }
    }

    /**
     * Xử lý sự kiện khi nhấn nút "Chơi lại".
     * Tên phương thức này phải trùng với giá trị onAction trong FXML.
     */
    @FXML
    private void handleRestart() {
        // Không cần gọi lại setupGrid() nếu cấu trúc GridPane không đổi
        initializeGame();
    }


    // --- Các phương thức logic game khác (processGuess, updateGrid, setMessage, endGame) ---
    // --- Giữ nguyên các phương thức này từ phiên bản trước ---
    // --- Đảm bảo chúng sử dụng các biến thành viên và @FXML fields đúng cách ---

    private Result[] processGuess(String guess) {
        Result[] results = new Result[WORD_LENGTH];
        Arrays.fill(results, Result.INCORRECT);

        Map<Character, Integer> secretLetterCounts = new HashMap<>();
        for (char c : secretWord.toCharArray()) {
            secretLetterCounts.put(c, secretLetterCounts.getOrDefault(c, 0) + 1);
        }

        // Bước 1: Check correct positions (Green)
        for (int i = 0; i < WORD_LENGTH; i++) {
            char guessChar = guess.charAt(i);
            if (guessChar == secretWord.charAt(i)) {
                results[i] = Result.CORRECT;
                secretLetterCounts.put(guessChar, secretLetterCounts.get(guessChar) - 1);
            }
        }

        // Bước 2: Check wrong positions (Yellow)
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (results[i] == Result.INCORRECT) {
                char guessChar = guess.charAt(i);
                if (secretLetterCounts.containsKey(guessChar) && secretLetterCounts.get(guessChar) > 0) {
                    results[i] = Result.WRONG_POSITION;
                    secretLetterCounts.put(guessChar, secretLetterCounts.get(guessChar) - 1);
                }
            }
        }
        return results;
    }

    private void updateGrid(String guess, Result[] feedback) {
        for (int i = 0; i < WORD_LENGTH; i++) {
            Label cell = letterCells[currentGuess][i];
            cell.setText(String.valueOf(guess.charAt(i)));
            cell.setTextFill(Color.WHITE);

            String style = "-fx-border-color: #7f8c8d; -fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3; -fx-background-color: ";
            switch (feedback[i]) {
                case CORRECT: style += "#2ecc71;"; break; // Green
                case WRONG_POSITION: style += "#f1c40f;"; break; // Yellow
                case INCORRECT: default: style += "#95a5a6;"; break; // Darker Gray
            }
            cell.setStyle(style);
        }
    }

    private void setMessage(String message, Color color) {
        messageLabel.setText(message);
        messageLabel.setTextFill(color);
    }

    private void endGame(boolean won) {
        guessInput.setDisable(true);
        guessButton.setDisable(true);
        restartButton.setVisible(true);
    }
}