package com.example.dictionary; // Thay đổi thành package của bạn

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.json.JSONObject; // Cần thư viện org.json

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class Googletab {

    @FXML
    private TextArea englishTextArea;

    @FXML
    private TextArea vietnameseTextArea;

    @FXML
    private Button translateButton;

    // Khởi tạo HttpClient (nên dùng lại thay vì tạo mới mỗi lần)
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    @FXML
    protected void handleTranslateButtonAction() {
        String textToTranslate = englishTextArea.getText();
        if (textToTranslate == null || textToTranslate.isBlank()) {
            vietnameseTextArea.setText(""); // Xóa kết quả nếu input trống
            return;
        }

        // Vô hiệu hóa nút và hiển thị trạng thái đang dịch
        translateButton.setDisable(true);
        vietnameseTextArea.setText("Translating...");

        // Gọi API dịch bất đồng bộ
        translateEnglishToVietnamese(textToTranslate)
                .thenAcceptAsync(translatedText -> {
                    // Cập nhật UI trên JavaFX Application Thread
                    Platform.runLater(() -> {
                        vietnameseTextArea.setText(translatedText);
                        translateButton.setDisable(false); // Kích hoạt lại nút
                    });
                }, Platform::runLater) // Đảm bảo callback chạy trên UI thread nếu cần (an toàn hơn)
                .exceptionally(error -> {
                    // Xử lý lỗi (ví dụ: lỗi mạng, lỗi API, lỗi parsing)
                    Platform.runLater(() -> {
                        // In chi tiết lỗi ra console để debug
                        System.err.println("Translation Error Occurred:");
                        // In stack trace để biết nguồn gốc lỗi
                        if (error.getCause() != null) {
                            error.getCause().printStackTrace();
                        } else {
                            error.printStackTrace();
                        }
                        // Hiển thị thông báo lỗi thân thiện hơn cho người dùng
                        String errorMessage = "Error: Could not translate.";
                        if (error.getCause() instanceof IllegalArgumentException) {
                            errorMessage += "\nDetails: Invalid character in URL (Check encoding).";
                        } else if (error.getCause() != null) {
                            errorMessage += "\nDetails: " + error.getCause().getMessage();
                        } else {
                            errorMessage += "\nDetails: " + error.getMessage();
                        }

                        vietnameseTextArea.setText(errorMessage);
                        translateButton.setDisable(false); // Kích hoạt lại nút khi có lỗi
                    });
                    return null; // Bắt buộc phải return trong exceptionally
                });
    }

    /**
     * Gửi yêu cầu dịch đến API MyMemory.
     *
     * @param englishText Văn bản tiếng Anh cần dịch.
     * @return CompletableFuture chứa chuỗi tiếng Việt đã dịch, hoặc ném Exception nếu lỗi.
     */
    private CompletableFuture<String> translateEnglishToVietnamese(String englishText) {
        try {
            // Mã hóa văn bản để đưa vào URL query
            String encodedText = URLEncoder.encode(englishText, StandardCharsets.UTF_8);
            String langPair = "en|vi"; // Dịch từ Anh (en) sang Việt (vi)

            // ----- SỬA ĐỔI QUAN TRỌNG Ở ĐÂY -----
            // Mã hóa cả giá trị của langPair để dấu '|' thành '%7C'
            String encodedLangPair = URLEncoder.encode(langPair, StandardCharsets.UTF_8);
            // ------------------------------------

            // Tạo chuỗi URL sử dụng các giá trị đã được mã hóa
            String apiUrl = String.format("https://api.mymemory.translated.net/get?q=%s&langpair=%s",
                    encodedText, encodedLangPair); // <-- Sử dụng encodedLangPair

            // Tạo HTTP Request (GET)
            // URI.create giờ sẽ nhận được URL hợp lệ
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET() // Phương thức GET là mặc định, nhưng ghi rõ ràng cũng tốt
                    .header("Accept", "application/json") // Yêu cầu kết quả dạng JSON
                    .build();

            // Gửi request bất đồng bộ và xử lý response
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            // Phân tích JSON response để lấy nội dung dịch
                            return parseTranslationResponse(response.body());
                        } else {
                            // Ném lỗi nếu response không thành công
                            // Bao gồm cả body để dễ debug hơn
                            throw new RuntimeException("API request failed with status code: " + response.statusCode() + "\nBody: " + response.body());
                        }
                    });

        } catch (Exception e) {
            // Bắt các lỗi khác có thể xảy ra khi tạo request (ví dụ: lỗi trong URI.create nếu apiUrl vẫn lỗi)
            System.err.println("Error creating HTTP request: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Phân tích cú pháp JSON trả về từ API MyMemory.
     *
     * @param jsonResponse Chuỗi JSON từ API.
     * @return Chuỗi tiếng Việt đã dịch.
     */
    private String parseTranslationResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            // Kiểm tra xem responseData và translatedText có tồn tại không
            if (jsonObject.has("responseData") && jsonObject.getJSONObject("responseData").has("translatedText")) {
                String translatedText = jsonObject.getJSONObject("responseData").getString("translatedText");
                // (Optional) MyMemory đôi khi trả về HTML entities, bạn có thể muốn decode chúng
                // Ví dụ sử dụng thư viện Apache Commons Text (cần thêm dependency)
                // import org.apache.commons.text.StringEscapeUtils;
                // return StringEscapeUtils.unescapeHtml4(translatedText);
                return translatedText; // Trả về trực tiếp nếu không cần decode HTML
            } else {
                // Nếu cấu trúc JSON không như mong đợi
                System.err.println("Unexpected JSON structure: 'responseData' or 'translatedText' missing.");
                System.err.println("Response Body: " + jsonResponse);
                throw new RuntimeException("Could not parse translation from response: Unexpected structure.");
            }
        } catch (Exception e) { // Bắt lỗi cụ thể hơn như JSONException nếu muốn
            // Ném lỗi nếu cấu trúc JSON không đúng như mong đợi hoặc có lỗi parsing
            System.err.println("JSON Parsing Error: " + e.getMessage());
            System.err.println("Response Body: " + jsonResponse); // In ra body để debug
            // Ném lại lỗi với thông tin rõ ràng hơn
            throw new RuntimeException("Could not parse translation response. Check console for details.", e);
        }
    }

    // (Optional) Bạn có thể thêm một phương thức initialize nếu cần
    @FXML
    public void initialize() {
        // Code khởi tạo nếu cần (ví dụ: đặt giá trị mặc định)
        System.out.println("Translation Controller Initialized.");
        // Có thể thêm gợi ý vào ô nhập liệu
        englishTextArea.setPromptText("Enter English text here...");
        vietnameseTextArea.setPromptText("Translation will appear here...");
    }
}