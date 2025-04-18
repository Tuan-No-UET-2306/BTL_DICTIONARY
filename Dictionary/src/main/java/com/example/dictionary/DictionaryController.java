package com.example.dictionary; // Thay đổi package cho phù hợp

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javafx.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Thư viện JSON ví dụ (thêm dependency vào project của bạn, ví dụ Gson hoặc Jackson)
// import com.google.gson.Gson;
// import com.google.gson.JsonArray;
// import com.google.gson.JsonObject;

public class DictionaryController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton; // Nút "Thêm từ" nhưng dùng để search

    @FXML
    private Label wordDisplayLabel;

    // Nút âm thanh đã được loại bỏ khỏi FXML và Controller

    @FXML
    private ListView<String> wordListView;

    @FXML
    private TextArea definitionArea;
    @FXML
    private Button backButton;
    @FXML
    void handleBack(ActionEvent event) {
        try {
            // 1. Lấy Stage hiện tại từ nút backButton (hoặc bất kỳ Node nào trên Scene)
            Stage stage = (Stage) backButton.getScene().getWindow();

            // 2. Tạo FXMLLoader để tải main.fxml
            // !!! QUAN TRỌNG: Đảm bảo đường dẫn "/com/example/dictionary/main.fxml" là chính xác
            // dựa trên vị trí của main.fxml trong thư mục resources.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/dictionary/main.fxml"));
            Parent root = loader.load(); // Tải FXML

            // 3. Tạo Scene mới
            Scene scene = new Scene(root);

            // 4. Đặt Scene mới cho Stage
            stage.setScene(scene);
            stage.setTitle("Main Application"); // Tùy chọn: Cập nhật tiêu đề cửa sổ
            stage.show(); // Hiển thị lại stage với scene mới

        } catch (IOException e) {
            System.err.println("Lỗi khi tải main.fxml: " + e.getMessage());
            e.printStackTrace();
            // Cân nhắc hiển thị thông báo lỗi cho người dùng
        }
    }



    // Danh sách từ gốc (có thể đọc từ file hoặc database)
    private final ObservableList<String> masterWordList = FXCollections.observableArrayList();
    // Danh sách từ được lọc để hiển thị trên ListView
    private FilteredList<String> filteredWordList;

    // Dùng để chạy các tác vụ mạng (API calls) ở background
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Không còn biến lưu URL âm thanh hoặc MediaPlayer

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Tải dữ liệu từ điển ban đầu (ví dụ)
        loadSampleDictionaryData(); // Thay thế bằng logic tải dữ liệu thực tế

        // 2. Setup FilteredList để lọc ListView khi gõ vào searchField
        filteredWordList = new FilteredList<>(masterWordList, p -> true); // Ban đầu hiển thị tất cả
        wordListView.setItems(filteredWordList);

        // Thêm listener cho searchField để lọc danh sách khi người dùng gõ
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterWordList(newValue);
        });

        // 3. Thêm listener cho ListView để hiển thị định nghĩa khi chọn từ
        wordListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                displayWordDefinition(newValue);
            } else {
                clearDefinition(); // Xóa khi không có gì được chọn
            }
        });

        // 4. Cấu hình ban đầu
        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
        // Không còn cấu hình cho nút âm thanh
    }

    // Hàm tải dữ liệu mẫu (thay thế bằng logic tải thực tế)
    private void loadSampleDictionaryData() {
        masterWordList.addAll("hello");
        FXCollections.sort(masterWordList);
    }

    // Hàm lọc danh sách từ dựa trên input
    private void filterWordList(String filter) {
        if (filter == null || filter.isEmpty()) {
            filteredWordList.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = filter.toLowerCase();
            filteredWordList.setPredicate(word -> word.toLowerCase().startsWith(lowerCaseFilter));
        }
        if (!filteredWordList.isEmpty()) {
            Platform.runLater(() -> wordListView.getSelectionModel().selectFirst());
        } else {
            Platform.runLater(() -> wordListView.getSelectionModel().clearSelection());
        }
    }

    // Hàm xử lý sự kiện nhấn nút "Thêm từ" (thực tế là tra cứu) hoặc Enter
    @FXML
    private void handleSearchAction() {
        String searchTerm = searchField.getText().trim();
        if (!searchTerm.isEmpty()) {
            boolean found = false;
            for (String word : filteredWordList) {
                if (word.equalsIgnoreCase(searchTerm)) {
                    wordListView.getSelectionModel().select(word);
                    wordListView.scrollTo(word);
                    found = true;
                    break;
                }
            }
            if (!found) {
                fetchDefinitionFromApi(searchTerm);
            }
        } else {
            wordListView.getSelectionModel().clearSelection();
            clearDefinition();
        }
    }

    // Hàm hiển thị định nghĩa (gọi API nếu cần)
    private void displayWordDefinition(String word) {
        wordDisplayLabel.setText("@" + word);
        definitionArea.setText("Đang tải định nghĩa cho '" + word + "'...");
        // Không còn xử lý nút âm thanh

        // **GỌI API Ở ĐÂY (TRONG BACKGROUND THREAD)**
        fetchDefinitionFromApi(word);
    }

    // Hàm gọi API để lấy định nghĩa
    private void fetchDefinitionFromApi(String word) {
        executorService.submit(() -> {
            try {
                // ----- BẮT ĐẦU LOGIC GỌI API -----
                String apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/" + URLEncoder.encode(word, StandardCharsets.UTF_8);
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                System.out.println("API Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Phân tích JSON để lấy định nghĩa
                    String definitionText = parseDefinitionFromJson(response.toString());
                    // Không còn phân tích URL âm thanh

                    // Cập nhật UI trên JavaFX Application Thread
                    Platform.runLater(() -> {
                        definitionArea.setText(definitionText);
                        // Không còn cập nhật nút âm thanh
                        // Cập nhật wordDisplayLabel nếu API trả về từ chuẩn hóa (tùy chọn)
                        // wordDisplayLabel.setText("@" + parsedWord);
                    });

                } else {
                    Platform.runLater(() -> {
                        definitionArea.setText("Không tìm thấy định nghĩa cho '" + word + "' (Lỗi: " + responseCode + ")");
                        wordDisplayLabel.setText("@" + word); // Vẫn hiện từ đã nhập/chọn
                        // Không còn xử lý nút âm thanh
                    });
                }
                conn.disconnect();
                // ----- KẾT THÚC LOGIC GỌI API -----

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    definitionArea.setText("Lỗi khi tra cứu '" + word + "': " + e.getMessage());
                    wordDisplayLabel.setText("@" + word);
                    // Không còn xử lý nút âm thanh
                });
            }
        });
    }

    // **Hàm ví dụ phân tích JSON để lấy định nghĩa (Cần tùy chỉnh theo API)**
    private String parseDefinitionFromJson(String jsonResponse) {
        // *** Cảnh báo: Code giả định, CẦN thay đổi dựa trên cấu trúc JSON thực tế ***
        StringBuilder result = new StringBuilder();
        try {
            if (jsonResponse.contains("\"definition\":\"")) {
                String[] parts = jsonResponse.split("\"definition\":\"");
                for (int i = 1; i < parts.length; i++) {
                    String def = parts[i].substring(0, parts[i].indexOf("\""));
                    result.append("- ").append(def).append("\n");
                }
            }
            if(result.length() == 0) return "Không tìm thấy định nghĩa chi tiết.";
            return result.toString();
        } catch (Exception e) {
            System.err.println("Lỗi phân tích JSON định nghĩa: " + e.getMessage());
            return "Lỗi khi phân tích định nghĩa.";
        }
    }

    // Hàm parseSoundUrlFromJson đã bị loại bỏ

    // Hàm handlePlaySoundAction đã bị loại bỏ

    // Hàm xóa nội dung vùng định nghĩa và label từ
    private void clearDefinition() {
        wordDisplayLabel.setText("");
        definitionArea.clear();
        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
        // Không còn xử lý nút âm thanh hoặc media player
    }

    // Hàm hiển thị thông báo lỗi đơn giản
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Hàm để đóng ExecutorService khi ứng dụng tắt (quan trọng)
    public void shutdown() {
        executorService.shutdown();
    }

//
//    public void handle(javafx.event.ActionEvent actionEvent) {
//    }
}
