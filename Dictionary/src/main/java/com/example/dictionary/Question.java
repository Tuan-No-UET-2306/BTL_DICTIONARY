package com.example.dictionary; // Hoặc package phù hợp của bạn

import java.util.Arrays; // Vẫn cần cho Arrays.copyOf

public class Question {
    private final String questionText;
    private final String[] options;
    private final String correctAnswer;

    // Constructor
    public Question(String questionText, String[] options, String correctAnswer) {
        this.questionText = questionText;
        // Tạo một bản sao của mảng options để đảm bảo tính bất biến (encapsulation)
        this.options = Arrays.copyOf(options, options.length);
        this.correctAnswer = correctAnswer;
    }

    // Getters
    public String getQuestionText() {
        return questionText;
    }

    public String[] getOptions() {
        // Trả về một bản sao để ngăn chặn việc thay đổi mảng options từ bên ngoài
        return Arrays.copyOf(options, options.length);
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    // --- PHƯƠNG THỨC MỚI ĐỂ KIỂM TRA ĐÁP ÁN ---
    /**
     * Kiểm tra xem đáp án được chọn có khớp với đáp án đúng của câu hỏi này không.
     * @param chosenAnswer Đáp án mà người dùng đã chọn.
     * @return true nếu đáp án đúng, false nếu sai.
     */
    public boolean isCorrect(String chosenAnswer) {
        if (chosenAnswer == null) { // Xử lý trường hợp đáp án chọn là null
            return false;
        }
        // So sánh trực tiếp đáp án được chọn với đáp án đúng của câu hỏi này
        return this.correctAnswer.equals(chosenAnswer);
    }
    // --- KẾT THÚC PHƯƠNG THỨC MỚI ---
}