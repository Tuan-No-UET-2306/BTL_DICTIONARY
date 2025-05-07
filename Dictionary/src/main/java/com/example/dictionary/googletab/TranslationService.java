package com.example.dictionary.googletab; // Package mới

import org.json.JSONObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;

/**
 * Dịch vụ gọi API dịch thuật MyMemory.
 * Nhiệm vụ: Gửi yêu cầu dịch và phân tích JSON phản hồi.
 * KHÔNG tương tác trực tiếp với UI.
 */
public class TranslationService {

    // Khởi tạo HttpClient (nên dùng lại thay vì tạo mới mỗi lần)
    // Sử dụng biến final private để đảm bảo thread-safe
    private final HttpClient httpClient;
    private static final String API_BASE_URL = "https://api.mymemory.translated.net/get?q=%s&langpair=%s";

    // Constructor
    public TranslationService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /**
     * Dịch văn bản từ ngôn ngữ nguồn sang ngôn ngữ đích sử dụng API MyMemory.
     *
     * @param text Văn bản cần dịch.
     * @param sourceLang Mã ngôn ngữ nguồn (ví dụ: "en").
     * @param targetLang Mã ngôn ngữ đích (ví dụ: "vi").
     * @return CompletableFuture chứa chuỗi văn bản đã dịch, hoặc thất bại với một Exception nếu lỗi.
     */
    public CompletableFuture<String> translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) {
            // Trả về CompletableFuture thành công với chuỗi rỗng nếu văn bản trống
            return CompletableFuture.completedFuture("");
        }

        try {
            // Mã hóa văn bản và langpair để đưa vào URL query
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String langPair = sourceLang + "|" + targetLang;
            String encodedLangPair = URLEncoder.encode(langPair, StandardCharsets.UTF_8);

            // Tạo chuỗi URL
            String apiUrl = String.format(API_BASE_URL, encodedText, encodedLangPair);

            // Tạo HTTP Request (GET)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .header("Accept", "application/json") // Yêu cầu kết quả dạng JSON
                    .timeout(java.time.Duration.ofSeconds(10)) // Thêm timeout cho request
                    .build();

            // Gửi request bất đồng bộ và xử lý response
            // thenApply sẽ chạy trên luồng hoàn thành sendAsync (thường là luồng của HttpClient)
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        // Kiểm tra mã trạng thái HTTP
                        if (response.statusCode() == 200) {
                            // Phân tích JSON response
                            return parseTranslationResponse(response.body());
                        } else {
                            // Ném lỗi nếu response không thành công (ví dụ: 404, 500...)
                            // Bao gồm cả body để dễ debug hơn
                            throw new RuntimeException("API request failed with status code: " + response.statusCode() + "\nBody: " + response.body());
                        }
                    });

        } catch (Exception e) {
            // Bắt các lỗi xảy ra trong quá trình tạo request (trước khi gửi sendAsync)
            // Ví dụ: Lỗi mã hóa URL, lỗi tạo URI...
            System.err.println("Error creating HTTP request for translation: " + e.getMessage());
            // Trả về CompletableFuture đã thất bại với nguyên nhân gốc
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Phân tích cú pháp JSON trả về từ API MyMemory để lấy văn bản đã dịch.
     * Phương thức này KHÔNG tương tác trực tiếp với UI.
     *
     * @param jsonResponse Chuỗi JSON từ API.
     * @return Chuỗi văn bản đã dịch.
     * @throws RuntimeException Nếu cấu trúc JSON không như mong đợi hoặc có lỗi parsing.
     */
    private String parseTranslationResponse(String jsonResponse) {
        try {
            // org.json.JSONObject có thể ném JSONException
            JSONObject jsonObject = new JSONObject(jsonResponse);

            // Kiểm tra trạng thái response từ API MyMemory (nếu có)
            // MyMemory trả về một field "responseStatus"
            if (jsonObject.has("responseStatus") && jsonObject.getInt("responseStatus") != 200) {
                String responseDetails = jsonObject.has("responseDetails") ? jsonObject.getString("responseDetails") : "Unknown API error";
                throw new RuntimeException("MyMemory API returned error status: " + jsonObject.getInt("responseStatus") + " - " + responseDetails);
            }


            // Kiểm tra xem responseData và translatedText có tồn tại không
            if (jsonObject.has("responseData") && jsonObject.getJSONObject("responseData").has("translatedText")) {
                String translatedText = jsonObject.getJSONObject("responseData").getString("translatedText");
                return translatedText; // Trả về trực tiếp nếu không cần decode HTML
            } else {
                // Nếu cấu trúc JSON không như mong đợi
                System.err.println("Unexpected JSON structure or missing data in response.");
                System.err.println("Response Body: " + jsonResponse);
                throw new RuntimeException("Could not parse translation from response: Unexpected structure or data.");
            }
        } catch (Exception e) { // Bắt JSONException và các Exception khác
            // Ném lại lỗi với thông tin rõ ràng hơn và bao gồm nguyên nhân gốc
            System.err.println("JSON Parsing Error: " + e.getMessage());
            System.err.println("Response Body: " + jsonResponse); // In ra body để debug
            throw new RuntimeException("Could not parse translation response. Check console for details.", e);
        }
    }
}