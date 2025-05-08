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

    public String guiYeuCau(String word) throws IOException {
        if (word == null || word.trim().isEmpty()) {
            throw new IllegalArgumentException("Từ không thể rỗng hoặc vô giá trị.");
        }
        String chuyenWord = URLEncoder.encode(word.trim(), StandardCharsets.UTF_8);
        String apiUrl = API_BASE_URL + chuyenWord;

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        System.out.println("Mã phản hồi API cho '" + word + "': " + responseCode);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                (responseCode == HttpURLConnection.HTTP_OK) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {

            String inputLine;
            StringBuilder noiLine = new StringBuilder();
            while ((inputLine = reader.readLine()) != null) {
                noiLine.append(inputLine);
            }
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NOT_FOUND) {
                throw new IOException("Mã lỗi: " + responseCode + ". Body: " + noiLine.toString());
            }

            return noiLine.toString();

        } // đảm bảo kết nối luôn đưuocj đóng sau khi sử dụng
        finally {
            conn.disconnect();
        }
    }
    //ktra xem chuỗi JSON thô có phản hồi lỗi không.
    public boolean isNotFoundResponse(String rawJsonResponse) {
        return rawJsonResponse != null && (rawJsonResponse.contains("\"title\":\"No Definitions Found\"") || rawJsonResponse.contains("\"message\":"));
    }
}
