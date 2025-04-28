package com.example.dictionary; // Thay đổi package cho phù hợp

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream; // Import cần thiết để đọc file từ resources
import java.io.InputStreamReader; // Import cần thiết
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap; // Import cho HashMap
import java.util.Map;     // Import cho Map
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// NOTE: Để phân tích JSON từ API một cách đáng tin cậy, bạn NÊN thêm dependency
//       vào project của mình và sử dụng một thư viện JSON chuyên nghiệp như Gson hoặc Jackson.
//       Ví dụ thêm dependency Gson vào pom.xml (Maven):
//       <dependency>
//           <groupId>com.google.code.gson</groupId>
//           artifactId>gson</artifactId>
//           <version>2.10.1</version> <!-- Sử dụng phiên bản mới nhất -->
//       </dependency>

// import com.google.gson.Gson;
// import com.google.gson.JsonArray;
// import com.google.gson.JsonObject;
// import com.google.gson.JsonParser; // Hoặc JsonParser cho Gson cũ hơn


public class DictionaryController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton; // Nút này dùng để kích hoạt tra cứu khi nhấn (ngoài việc nhấn Enter)

    @FXML
    private Label wordDisplayLabel;

    @FXML
    private ListView<String> wordListView;

    @FXML
    private TextArea definitionArea;

    @FXML
    private Button backButton;

    // --- Biến lưu trữ dữ liệu ---
    // Danh sách từ gốc (chỉ chứa các từ tiếng Anh, đọc từ file dic_words.txt)
    private final ObservableList<String> masterWordList = FXCollections.observableArrayList();
    // Danh sách từ được lọc để hiển thị trên ListView
    private FilteredList<String> filteredWordList;

    // Map lưu trữ từ và TOÀN BỘ phần còn lại của dòng từ file (định nghĩa thô)
    // Key: Từ vựng (String), Value: Toàn bộ chuỗi sau từ (bao gồm phiên âm, loại từ, định nghĩa...)
    private final Map<String, String> localDictionaryRaw = new HashMap<>();


    // --- Service cho các tác vụ nền ---
    // Dùng để chạy các tác vụ mạng (API calls) ở background
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Không còn biến lưu URL âm thanh hoặc MediaPlayer

    // --- Phương thức initialize ---
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Tải dữ liệu từ điển ban đầu (từ file dic_words.txt)
        loadDictionaryWords(); // <-- Gọi hàm tải dữ liệu từ file

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
                // Khi chọn từ từ list, hiển thị định nghĩa từ dữ liệu cục bộ (với format)
                displayFormattedLocalDefinition(newValue); // <-- Gọi hàm hiển thị ĐỊNH NGHĨA CỤC BỘ ĐÃ FORMAT
            } else {
                clearDefinition(); // Xóa khi không có gì được chọn
            }
        });

        // 4. Cấu hình ban đầu cho TextArea
        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
        definitionArea.setEditable(false); // Ngăn chỉnh sửa định nghĩa

        // 5. Liên kết Enter trong searchField và nút searchButton với hành động handleSearchAction
        searchField.setOnAction(event -> handleSearchAction());
        searchButton.setOnAction(event -> handleSearchAction()); // Liên kết nút searchButton
    }

    // --- Logic xử lý sự kiện ---

    @FXML
    void handleBack(ActionEvent event) {
        // Xử lý sự kiện nút Back
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            // !!! QUAN TRỌNG: Đảm bảo đường dẫn "/com/example/dictionary/main.fxml" là chính xác
            // dựa trên vị trí của main.fxml trong thư mục resources.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/dictionary/main.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Main Application");
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi khi tải main.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi", "Không thể quay lại màn hình chính.");
        }
    }

    @FXML
    private void handleSearchAction() {
        // Xử lý sự kiện nhấn nút Search hoặc Enter trong searchField
        String searchTerm = searchField.getText().trim();
        if (!searchTerm.isEmpty()) {
            // Khi nhấn Search/Enter, thử hiển thị định nghĩa cục bộ trước
            boolean foundLocally = displayFormattedLocalDefinition(searchTerm); // <-- Dùng hàm hiển thị FORMAT

            // Nếu không tìm thấy cục bộ, thử tra cứu API
            if (!foundLocally) {
                fetchDefinitionFromApi(searchTerm); // <-- Vẫn giữ API làm dự phòng
            }
        } else {
            // Nếu ô tìm kiếm trống, xóa chọn và định nghĩa
            wordListView.getSelectionModel().clearSelection();
            clearDefinition();
        }
    }


    // --- Các hàm helper ---

    // Hàm tải dữ liệu từ điển từ file dic_words.txt trong resources
    // Phân tích từng dòng để lấy từ và định nghĩa thô
    private void loadDictionaryWords() {
        // !!! QUAN TRỌNG: Đảm bảo đường dẫn này khớp với vị trí của file trong thư mục resources
        // Dựa trên cấu trúc thư mục phổ biến, file nằm trong src/main/resources/com.example.dictionary/dic_words.txt
        String resourcePath = "/com/example/dictionary/dic_words.txt"; // <--- Tên file dic_words.txt của bạn

        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            if (is == null) {
                System.err.println("Không tìm thấy file resource: " + resourcePath);
                showAlert("Lỗi tải dữ liệu", "Không tìm thấy file từ điển: " + resourcePath);
                return; // Thoát nếu không tìm thấy file
            }

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Xóa khoảng trắng đầu cuối
                if (!line.isEmpty()) {
                    // --- PHÂN TÍCH DÒNG ĐỂ LẤY TỪ VÀ LƯU TOÀN BỘ CHUỖI THÔ CÒN LẠI ---
                    // Dựa trên định dạng: "từ /phiên âm/ \*loại từ\* định nghĩa \\"
                    // Tìm vị trí của dấu cách hoặc / đầu tiên để tách từ
                    int firstSpaceOrSlashIndex = -1;
                    int spaceIndex = line.indexOf(' ');
                    int slashIndex = line.indexOf('/');

                    if (spaceIndex != -1 && slashIndex != -1) {
                        firstSpaceOrSlashIndex = Math.min(spaceIndex, slashIndex);
                    } else if (spaceIndex != -1) {
                        firstSpaceOrSlashIndex = spaceIndex;
                    } else if (slashIndex != -1) {
                        firstSpaceOrSlashIndex = slashIndex;
                    }

                    String word = "";
                    String rawDefinitionPart = ""; // Chuỗi chứa phiên âm, loại từ, định nghĩa...

                    if (firstSpaceOrSlashIndex != -1) {
                        word = line.substring(0, firstSpaceOrSlashIndex).trim();
                        rawDefinitionPart = line.substring(firstSpaceOrSlashIndex).trim();
                    } else {
                        // Nếu không tìm thấy dấu cách/slash, coi toàn bộ dòng là từ (trường hợp không có phiên âm/định nghĩa?)
                        word = line.trim();
                        rawDefinitionPart = ""; // Không có phần định nghĩa thô
                    }

                    if (!word.isEmpty()) {
                        // Lưu vào Map TOÀN BỘ CHUỖI THÔ CÒN LẠI
                        localDictionaryRaw.put(word, rawDefinitionPart);
                        // Thêm từ vào danh sách hiển thị trên ListView
                        masterWordList.add(word);
                        count++;
                    } else {
                        System.err.println("Bỏ qua dòng không lấy được từ: " + line);
                    }
                    // -----------------------------------------------------------
                }
            }
            System.out.println("Đã tải " + count + " từ (có dữ liệu định nghĩa cục bộ) vào danh sách gợi ý.");

        } catch (IOException e) {
            System.err.println("Lỗi khi đọc file từ điển: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi tải dữ liệu", "Không thể đọc file từ điển: " + e.getMessage());
        }

        // Sắp xếp danh sách từ sau khi tải xong (quan trọng cho FilteredList và tìm kiếm)
        FXCollections.sort(masterWordList);
    }

    // --- HÀM MỚI: HIỂN THỊ ĐỊNH NGHĨA TỪ DỮ LIỆU CỤC BỘ VỚI FORMAT ---
    // Phân tích chuỗi định nghĩa thô và hiển thị có cấu trúc.
    // Trả về true nếu tìm thấy dữ liệu thô và hiển thị, false nếu không có dữ liệu thô cho từ này
    private boolean displayFormattedLocalDefinition(String word) {
        if (word == null || word.trim().isEmpty()) {
            clearDefinition();
            return true; // Coi như xử lý xong (không có từ để hiển thị)
        }
        String cleanedWord = word.trim();

        // Lấy chuỗi định nghĩa thô từ Map
        String rawDefinition = localDictionaryRaw.get(cleanedWord);

        if (rawDefinition != null && !rawDefinition.isEmpty()) {
            // Tìm thấy dữ liệu thô cục bộ, tiến hành phân tích và format
            StringBuilder formattedText = new StringBuilder();

            // Bắt đầu với Từ vựng và Phiên âm
            // Phiên âm nằm giữa hai dấu '/' đầu tiên
            int firstSlash = rawDefinition.indexOf('/');
            int secondSlash = rawDefinition.indexOf('/', firstSlash + 1);

            String pronunciation = "";
            String restOfLine = rawDefinition; // Phần còn lại sau khi xử lý phiên âm

            if (firstSlash != -1 && secondSlash != -1) {
                pronunciation = rawDefinition.substring(firstSlash, secondSlash + 1).trim(); // Bao gồm dấu '/'
                restOfLine = rawDefinition.substring(secondSlash + 1).trim();
            } else {
                // Không tìm thấy phiên âm, coi toàn bộ là phần còn lại
                restOfLine = rawDefinition;
            }

            // Dòng 1: Từ vựng và Phiên âm
            formattedText.append(cleanedWord);
            if (!pronunciation.isEmpty()) {
                formattedText.append(" ").append(pronunciation);
            }
            formattedText.append("\n"); // Xuống dòng


            // Tìm Phần loại từ và Định nghĩa
            // Loại từ nằm sau phiên âm và trước dấu "\*"
            int starIndex = restOfLine.indexOf("\\*"); // Tìm vị trí của "\*" trong phần còn lại

            String partOfSpeech = "";
            String definitionsPart = ""; // Chuỗi chứa các định nghĩa gạch đầu dòng

            if (starIndex != -1) {
                partOfSpeech = restOfLine.substring(0, starIndex).trim();
                definitionsPart = restOfLine.substring(starIndex + 2).trim(); // Bỏ qua "\*"
            } else {
                // Không tìm thấy "\*", coi toàn bộ phần còn lại là định nghĩa?
                // Hoặc có thể là loại từ không có dấu "*"? Dựa trên định dạng file, có vẻ *type* luôn có.
                // Nếu không có "\*", có thể dữ liệu không đúng format hoặc chỉ có từ/phiên âm.
                String partOfLine = restOfLine; // Giả định phần còn lại là loại từ hoặc gì đó
                definitionsPart = "";
            }

            // Xóa bỏ ký tự "\\\\" cuối cùng nếu có (từ định dạng file)
            if (definitionsPart.endsWith("\\\\")) {
                definitionsPart = definitionsPart.substring(0, definitionsPart.length() - 2).trim();
            }
            // Xóa bỏ các ký tự thoát khác nếu cần (ví dụ: "\-") trong định nghĩa
            definitionsPart = definitionsPart.replace("\\-", "-");


            // Dòng 2: Loại từ (ví dụ: danh từ, tính từ)
            if (!partOfSpeech.isEmpty()) {
                formattedText.append(partOfSpeech).append(":\n"); // Thêm dấu ":" và xuống dòng
            }

            // Các dòng tiếp theo: Các định nghĩa gạch đầu dòng
            // Các định nghĩa được phân tách bằng " - "
            if (!definitionsPart.isEmpty()) {
                String[] definitions = definitionsPart.split(" - ");
                for (String def : definitions) {
                    String trimmedDef = def.trim();
                    if (!trimmedDef.isEmpty()) {
                        // Thêm gạch đầu dòng và định nghĩa
                        formattedText.append("- ").append(trimmedDef).append("\n");
                    }
                }
            } else if (partOfSpeech.isEmpty()) {
                // Trường hợp không có cả loại từ lẫn định nghĩa chi tiết sau phiên âm
                formattedText.append("Không có định nghĩa chi tiết trong file.\n");
            }


            // Cập nhật UI trên JavaFX Application Thread
            Platform.runLater(() -> {
                wordDisplayLabel.setText("@" + cleanedWord);
                definitionArea.setText(formattedText.toString());
            });

            return true; // Đã tìm thấy dữ liệu thô và hiển thị
        } else {
            // Không tìm thấy dữ liệu thô cục bộ cho từ này
            Platform.runLater(() -> {
                wordDisplayLabel.setText("@" + cleanedWord);
                // Thông báo không tìm thấy cục bộ, chuẩn bị gọi API
                definitionArea.setText("Không tìm thấy định nghĩa cục bộ cho '" + cleanedWord + "'. Đang tra cứu online...");
            });
            return false; // Không tìm thấy cục bộ
        }
    }
    // --------------------------------------------------------------------


    // Hàm lọc danh sách từ dựa trên input từ searchField
    private void filterWordList(String filter) {
        if (filter == null || filter.isEmpty()) {
            filteredWordList.setPredicate(p -> true); // Hiển thị tất cả
        } else {
            String lowerCaseFilter = filter.toLowerCase();
            // Lọc các từ bắt đầu bằng chuỗi nhập vào (không phân biệt hoa thường)
            filteredWordList.setPredicate(word -> word.toLowerCase().startsWith(lowerCaseFilter));
        }

        // Tự động cuộn tới phần tử đầu tiên được hiển thị nếu danh sách lọc không rỗng
        if (!filteredWordList.isEmpty()) {
            Platform.runLater(() -> {
                // Cuộn tới phần tử đầu tiên của danh sách đã lọc
                wordListView.scrollTo(filteredWordList.get(0));
            });
        } else {
            // Nếu danh sách lọc rỗng, xóa chọn và định nghĩa
            Platform.runLater(() -> {
                wordListView.getSelectionModel().clearSelection();
                clearDefinition(); // Xóa định nghĩa cũ khi danh sách lọc rỗng
            });
        }
    }

    // Hàm hiển thị định nghĩa (gọi API nếu cần) - Giữ lại làm dự phòng
    private void fetchDefinitionFromApi(String word) {
        // Đảm bảo từ không rỗng trước khi gọi API
        if (word == null || word.trim().isEmpty()) {
            Platform.runLater(() -> definitionArea.setText("Từ cần tra cứu không hợp lệ cho API."));
            return;
        }
        String finalWord = word.trim();

        // Chạy tác vụ mạng trên background thread
        executorService.submit(() -> {
            String definitionText;
            int responseCode = -1; // Lưu mã phản hồi

            try {
                // Mã hóa từ để đảm bảo URL hợp lệ
                String encodedWord = URLEncoder.encode(finalWord, StandardCharsets.UTF_8);
                // Sử dụng API miễn phí từ dictionaryapi.dev (API tiếng Anh)
                String apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/" + encodedWord;
                URL url = new URL(apiUrl);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(5000); // Timeout kết nối 5 giây
                conn.setReadTimeout(5000);    // Timeout đọc dữ liệu 5 giây

                responseCode = conn.getResponseCode(); // Lấy mã phản hồi
                System.out.println("API Response Code for '" + finalWord + "': " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Đọc dữ liệu từ phản hồi
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // --- PHÂN TÍCH JSON ---
                    // *** CẢNH BÁO QUAN TRỌNG ***
                    // Logic phân tích JSON hiện tại DỰA VÀO CHUỖI và KHÔNG ĐÁNG TIN CẬY.
                    // API trả về một cấu trúc JSON phức tạp.
                    // CẦN sử dụng thư viện JSON (Gson, Jackson) để phân tích đúng.
                    // Code mẫu dưới đây chỉ là một attempt ĐƠN GIẢN VÀ CÓ THỂ SAI/THIẾU.
                    definitionText = parseDefinitionFromJson(response.toString());
                    // --- KẾT THÚC PHÂN TÍCH JSON ---

                    // Cập nhật UI trên JavaFX Application Thread với kết quả từ API
                    Platform.runLater(() -> {
                        // Nếu đã hiển thị định nghĩa cục bộ, thêm phần API vào dưới
                        // Nếu chưa, chỉ hiển thị kết quả từ API
                        if (localDictionaryRaw.containsKey(finalWord) && !definitionArea.getText().contains("Đang tra cứu online")) {
                            // Có định nghĩa cục bộ, thêm API vào cuối
                            definitionArea.appendText("\n\n--- Định nghĩa online ---\n" + definitionText);
                        } else {
                            // Không có định nghĩa cục bộ hoặc đang hiển thị thông báo chờ API, ghi đè
                            definitionArea.setText(definitionText);
                        }
                        wordDisplayLabel.setText("@" + finalWord); // Cập nhật lại label nếu cần
                    });


                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    definitionText = "Không tìm thấy định nghĩa online cho '" + finalWord + "'.";
                    Platform.runLater(() -> {
                        // Nếu không tìm thấy cục bộ và API cũng không tìm thấy
                        if(!localDictionaryRaw.containsKey(finalWord)){
                            definitionArea.setText("Không tìm thấy định nghĩa cho '" + finalWord + "' cả trong file cục bộ lẫn online.");
                        } else {
                            // Đã hiển thị định nghĩa cục bộ, chỉ thông báo không tìm thấy online
                            definitionArea.appendText("\n\n--- Không tìm thấy định nghĩa online ---");
                        }
                        wordDisplayLabel.setText("@" + finalWord);
                    });
                }
                else { // Các mã lỗi API khác
                    definitionText = "Không thể tra cứu định nghĩa online cho '" + finalWord + "' (Lỗi API: " + responseCode + ")";
                    Platform.runLater(() -> {
                        if(!localDictionaryRaw.containsKey(finalWord)){
                            definitionArea.setText(definitionText); // Ghi đè lỗi nếu không có cục bộ
                        } else {
                            definitionArea.appendText("\n\n--- Lỗi khi tra cứu online ---\n" + definitionText);
                        }
                        wordDisplayLabel.setText("@" + finalWord);
                    });
                }
                conn.disconnect();

            } catch (IOException e) {
                System.err.println("Lỗi IO khi gọi API cho '" + finalWord + "': " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    String errorMsg = "Lỗi mạng hoặc kết nối khi tra cứu online '" + finalWord + "': " + e.getMessage();
                    if(!localDictionaryRaw.containsKey(finalWord)){
                        definitionArea.setText(errorMsg);
                    } else {
                        definitionArea.appendText("\n\n--- Lỗi khi tra cứu online ---\n" + errorMsg);
                    }
                    wordDisplayLabel.setText("@" + finalWord);
                });
            } catch (Exception e) {
                System.err.println("Lỗi khác khi tra cứu API cho '" + finalWord + "': " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    String errorMsg = "Lỗi không xác định khi tra cứu online '" + finalWord + "': " + e.getMessage();
                    if(!localDictionaryRaw.containsKey(finalWord)){
                        definitionArea.setText(errorMsg);
                    } else {
                        definitionArea.appendText("\n\n--- Lỗi khi tra cứu online ---\n" + errorMsg);
                    }
                    wordDisplayLabel.setText("@" + finalWord);
                });
            }
        });
    }

    // **Hàm ví dụ phân tích JSON để lấy định nghĩa từ API (CẦN THAY THẾ BẰNG THƯ VIỆN JSON)**
    // Hàm này rất cơ bản và KHÔNG ĐÁNG TIN CẬY với cấu trúc JSON thực tế từ API.
    // Giữ lại logic phân tích thô cho trường hợp này.
    private String parseDefinitionFromJson(String jsonResponse) {
        // Sử dụng phương pháp phân tích chuỗi thô (rất dễ vỡ) như trước
        StringBuilder result = new StringBuilder();
        try {
            // Cố gắng phát hiện các phản hồi lỗi phổ biến từ API dictionaryapi.dev
            if (jsonResponse != null && (jsonResponse.contains("\"title\":\"No Definitions Found\"") || jsonResponse.contains("\"message\":\""))) {
                try {
                    // Cố gắng phân tích lỗi JSON cơ bản (không dùng thư viện)
                    // Tìm message trước
                    int msgIndex = jsonResponse.indexOf("\"message\":\"");
                    if (msgIndex != -1) {
                        int startIndex = msgIndex + "\"message\":\"".length();
                        int endIndex = jsonResponse.indexOf("\"", startIndex);
                        if (endIndex != -1) {
                            String message = jsonResponse.substring(startIndex, endIndex).replace("\\\"", "\""); // Xử lý thoát ký tự cơ bản
                            result.append("API báo lỗi: ").append(message).append("\n");
                        }
                    }

                    // Nếu chưa tìm được message, tìm title
                    if (result.length() == 0 && jsonResponse.contains("\"title\":\"")) {
                        int titleIndex = jsonResponse.indexOf("\"title\":\"");
                        if (titleIndex != -1) {
                            int startIndex = titleIndex + "\"title\":\"".length();
                            int endIndex = jsonResponse.indexOf("\"", startIndex);
                            if (endIndex != -1) {
                                String title = jsonResponse.substring(startIndex, endIndex).replace("\\\"", "\""); // Xử lý thoát ký tự cơ bản
                                result.append("API báo lỗi: ").append(title).append("\n");
                            }
                        }
                    }

                    // Nếu vẫn không tìm thấy thông báo lỗi cụ thể
                    if(result.length() == 0) {
                        result.append("API báo lỗi không rõ nguyên nhân hoặc không tìm thấy định nghĩa.");
                    }

                } catch (Exception e) {
                    System.err.println("Lỗi khi phân tích phản hồi lỗi JSON: " + e.getMessage());
                    result.append("API báo lỗi (không phân tích được thông báo chi tiết).");
                }
                return result.toString().trim(); // Trả về thông báo lỗi đã phân tích
            }


            // Nếu không phải phản hồi lỗi, cố gắng phân tích định nghĩa
            String lowerCaseResponse = jsonResponse.toLowerCase();
            int index = lowerCaseResponse.indexOf("\"definition\":\"");
            while (index != -1) {
                int startIndex = index + "\"definition\":\"".length();
                int endIndex = jsonResponse.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    String definition = jsonResponse.substring(startIndex, endIndex);
                    // Xử lý các ký tự thoát cơ bản trong chuỗi JSON nếu cần (ví dụ: \" -> ")
                    definition = definition.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
                    result.append("- ").append(definition).append("\n");
                    // Bỏ qua các ký tự đến sau dấu đóng ngoặc kép hiện tại để tìm lần xuất hiện tiếp theo
                    // Bắt đầu tìm kiếm từ vị trí ngay sau endIndex
                    index = lowerCaseResponse.indexOf("\"definition\":\"", endIndex + 1); // +1 để tránh tìm lại chính dấu " vừa tìm thấy
                } else {
                    break; // Không tìm thấy dấu đóng ngoặc kép, dừng lại
                }
            }

            // Nếu vẫn không tìm thấy định nghĩa sau khi phân tích
            if (result.length() == 0) {
                // Cố gắng tìm các trường khác như "meaning", "partOfSpeech" để cung cấp thông tin cơ bản nếu không có định nghĩa chi tiết
                // Ví dụ: tìm "partOfSpeech":"noun"
                if (lowerCaseResponse.contains("\"partofspeech\":\"")) {
                    try { // Cố gắng trích xuất loại từ một cách thô sơ
                        int psIndex = lowerCaseResponse.indexOf("\"partofspeech\":\"") + "\"partofspeech\":\"".length();
                        int psEndIndex = lowerCaseResponse.indexOf("\"", psIndex);
                        if (psEndIndex != -1) {
                            String partOfSpeech = jsonResponse.substring(psIndex, psEndIndex);
                            result.append("Tìm thấy từ, nhưng không có định nghĩa chi tiết (Kiểu từ: ").append(partOfSpeech).append(").\n");
                        } else {
                            result.append("Tìm thấy từ, nhưng không có định nghĩa chi tiết.\n");
                        }
                    } catch (Exception parseE) {
                        System.err.println("Lỗi khi trích xuất partOfSpeech: " + parseE.getMessage());
                        result.append("Tìm thấy từ, nhưng không có định nghĩa chi tiết.\n");
                    }
                } else {
                    // Trường hợp không tìm thấy cả definition lẫn partOfSpeech
                    result.append("Không tìm thấy định nghĩa chi tiết trong phản hồi API.");
                }
            }
            return result.toString().trim(); // Loại bỏ khoảng trắng thừa ở cuối

        } catch (Exception e) {
            System.err.println("Lỗi khi phân tích JSON định nghĩa (phương pháp chuỗi): " + e.getMessage());
            e.printStackTrace();
            return "Lỗi khi phân tích định nghĩa từ phản hồi API.";
        }
         /*
         // *** NHẮC LẠI: VÍ DỤ CÁCH PHÂN TÍCH BẰNG Gson (nếu đã thêm dependency) ***
         // Đây là cách xử lý JSON chính xác và đáng tin cậy hơn nhiều!
         // Cần import com.google.gson.* và thêm dependency Gson vào project.
         // HÃY THAY THẾ TOÀN BỘ HÀM parseDefinitionFromJson bằng code tương tự như sau:
         // (Code này đã có trong câu trả lời trước)
         */
    }


    // Hàm xóa nội dung vùng định nghĩa và label từ
    private void clearDefinition() {
        wordDisplayLabel.setText("");
        definitionArea.clear();
        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
    }

    // Hàm hiển thị thông báo lỗi đơn giản
    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            System.out.println("Đóng ExecutorService...");
            executorService.shutdown();
        }
    }
}