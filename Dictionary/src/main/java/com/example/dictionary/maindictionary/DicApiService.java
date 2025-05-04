package com.example.dictionary.maindictionary;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DicApiService {

    private static final String API_BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/";

    /**
     * Gọi API để lấy định nghĩa cho một từ vựng.
     *
     * @param word Từ vựng cần tra cứu.
     * @return Chuỗi JSON phản hồi từ API.
     * @throws IOException Nếu có lỗi mạng hoặc HTTP response code không phải 200/404.
     */
    public String fetchDefinition(String word) throws IOException {
        if (word == null || word.trim().isEmpty()) {
            throw new IllegalArgumentException("Word cannot be null or empty.");
        }
        String encodedWord = URLEncoder.encode(word.trim(), StandardCharsets.UTF_8);
        String apiUrl = API_BASE_URL + encodedWord;

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000); // Timeout kết nối 5 giây
        conn.setReadTimeout(5000);    // Timeout đọc dữ liệu 5 giây

        int responseCode = conn.getResponseCode();
        System.out.println("API Response Code for '" + word + "': " + responseCode);

        // Xử lý response code (200 OK, 404 Not Found, hoặc các lỗi khác)
        // Chúng ta sẽ đọc body ngay cả khi 404 vì API vẫn trả về JSON giải thích lỗi
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                (responseCode == HttpURLConnection.HTTP_OK) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {

            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }

            // Nếu response code không OK và không phải 404, ném exception
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NOT_FOUND) {
                // Có thể đọc body response để có thêm thông tin về lỗi từ API
                throw new IOException("API returned non-OK response code: " + responseCode + ". Body: " + response.toString());
            }

            return response.toString(); // Trả về chuỗi JSON (hoặc JSON lỗi)

        } finally {
            conn.disconnect(); // Luôn đóng kết nối
        }
    }

    /**
     * Phương thức kiểm tra liệu phản hồi API có phải là "Không tìm thấy" (404) hay không.
     * Dựa vào cấu trúc JSON lỗi của dictionaryapi.dev.
     * @param rawJsonResponse Chuỗi JSON phản hồi từ API.
     * @return true nếu phản hồi chỉ ra không tìm thấy định nghĩa, false nếu ngược lại.
     */
    public boolean isNotFoundResponse(String rawJsonResponse) {
        // Đây là một cách kiểm tra thô dựa trên chuỗi. Sử dụng thư viện JSON tốt hơn.
        return rawJsonResponse != null && (rawJsonResponse.contains("\"title\":\"No Definitions Found\"") || rawJsonResponse.contains("\"message\":"));
        // message cũng có thể chỉ ra lỗi khác, nên check title chính xác hơn nếu có thể.
    }
}
