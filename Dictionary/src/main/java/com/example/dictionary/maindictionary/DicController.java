package com.example.dictionary.maindictionary;

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

import java.io.IOException;
// Không cần import InputStream/InputStreamReader/BufferedReader/URLEncoder/StandardCharsets ở đây nữa,
// vì các class Service/DataLoader sẽ xử lý chúng.
// Tuy nhiên, giữ lại import cho HttpURLConnection/URL nếu vẫn muốn sử dụng chúng trong thông báo lỗi chi tiết
import java.net.URL; // Có thể cần


import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Import các class mới từ package maindic


// NOTE: Để phân tích JSON từ API một cách đáng tin cậy trong DefinitionFormatter,
//       bạn NÊN thêm dependency vào project của mình và sử dụng một thư viện JSON chuyên nghiệp như Gson hoặc Jackson.


public class DicController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

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
    // Controller giữ reference để tra cứu nhanh cục bộ
    private Map<String, String> localDictionaryRaw; // Không final, sẽ được gán sau khi load


    // --- Các đối tượng chức năng (Services) ---
    private DicDataLoader dataLoader;
    private DicApiService apiService;
    private DefinitionFormatter definitionFormatter;


    // --- Service cho các tác vụ nền ---
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Không còn biến lưu URL âm thanh hoặc MediaPlayer

    // --- Phương thức initialize ---
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Khởi tạo các đối tượng chức năng từ cùng package maindic
        // !!! Đảm bảo đường dẫn "/com/example/dictionary/dic_words.txt" là chính xác (đường dẫn trong resources)
        // Đường dẫn này vẫn tính từ gốc thư mục resources, không phụ thuộc vào package của class code
        dataLoader = new DicDataLoader("/com/example/dictionary/dic_words.txt");
        apiService = new DicApiService();
        definitionFormatter = new DefinitionFormatter(); // Sử dụng formatter để phân tích/format

        // 2. Tải dữ liệu từ điển ban đầu (giao cho dataLoader)
        loadDictionaryWords(); // <-- Gọi hàm tải dữ liệu

        // 3. Setup FilteredList để lọc ListView khi gõ vào searchField
        filteredWordList = new FilteredList<>(masterWordList, p -> true); // Ban đầu hiển thị tất cả
        wordListView.setItems(filteredWordList);

        // 4. Thêm listener cho searchField để lọc danh sách khi người dùng gõ
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterWordList(newValue);
        });

        // 5. Thêm listener cho ListView để hiển thị định nghĩa khi chọn từ
        wordListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // Khi chọn từ từ list, hiển thị định nghĩa từ dữ liệu cục bộ (với format)
                displayWordDefinition(newValue); // Gọi phương thức hiển thị chính
            } else {
                clearDefinition(); // Xóa khi không có gì được chọn
            }
        });

        // 6. Cấu hình ban đầu cho TextArea
        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
        definitionArea.setEditable(false); // Ngăn chỉnh sửa định nghĩa

        // 7. Liên kết Enter trong searchField và nút searchButton với hành động handleSearchAction
        searchField.setOnAction(event -> handleSearchAction());
        searchButton.setOnAction(event -> handleSearchAction()); // Liên kết nút searchButton
    }

    // --- Logic xử lý sự kiện ---

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            // !!! QUAN TRỌNG: Đảm bảo đường dẫn "/com/example/dictionary/main.fxml" là chính xác trong resources
            // Đường dẫn này không phụ thuộc vào package của Controller hiện tại
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
        String searchTerm = searchField.getText().trim();
        if (!searchTerm.isEmpty()) {
            // Khi nhấn Search/Enter, gọi phương thức hiển thị chính
            displayWordDefinition(searchTerm);
        } else {
            wordListView.getSelectionModel().clearSelection();
            clearDefinition();
        }
    }

    // --- Phương thức hiển thị định nghĩa chính (Controller điều phối) ---
    // Phương thức này sẽ quyết định hiển thị định nghĩa cục bộ hay tra API
    private void displayWordDefinition(String word) {
        if (word == null || word.trim().isEmpty()) {
            clearDefinition();
            return;
        }
        String cleanedWord = word.trim();

        wordDisplayLabel.setText("@" + cleanedWord);
        definitionArea.setText("Đang xử lý '" + cleanedWord + "'..."); // Hiển thị trạng thái ban đầu

        // 1. Thử tra cứu định nghĩa cục bộ
        String rawLocalDef = localDictionaryRaw.get(cleanedWord);
        if (rawLocalDef != null) {
            // Nếu tìm thấy cục bộ, format và hiển thị
            String formattedDef = definitionFormatter.formatLocalDefinition(cleanedWord, rawLocalDef);
            Platform.runLater(() -> definitionArea.setText(formattedDef)); // Cập nhật UI ngay
        } else {
            // Nếu không tìm thấy cục bộ, thông báo và chuẩn bị gọi API
            Platform.runLater(() -> definitionArea.setText("Không tìm thấy định nghĩa cục bộ cho '" + cleanedWord + "'. Đang tra cứu online..."));
            // 2. Gọi API (Giao cho apiService)
            fetchOnlineDefinition(cleanedWord); // Hàm riêng để xử lý API
        }
    }


    // --- Các hàm helper ---

    // Hàm tải dữ liệu từ điển từ file dic_words.txt trong resources
    // Giờ đây chỉ gọi DataLoader
    private void loadDictionaryWords() {
        try {
            localDictionaryRaw = dataLoader.loadWordsAndRawDefinitions(); // DataLoader tải và trả về Map thô
            // Lấy danh sách từ từ các keys của Map và thêm vào ObservableList
            masterWordList.addAll(localDictionaryRaw.keySet());
            FXCollections.sort(masterWordList); // Sắp xếp danh sách từ

        } catch (RuntimeException e) { // Bắt RuntimeException mà DataLoader ném ra khi lỗi IO
            System.err.println("Lỗi tải dữ liệu từ điển: " + e.getMessage());
            showAlert("Lỗi tải dữ liệu", "Không thể tải dữ liệu từ điển ban đầu: " + e.getMessage());
            // masterWordList và localDictionaryRaw sẽ rỗng hoặc một phần, ứng dụng vẫn chạy nhưng không có gợi ý/định nghĩa cục bộ
            localDictionaryRaw = new HashMap<>(); // Đảm bảo map không null ngay cả khi lỗi
            masterWordList.clear(); // Đảm bảo list rỗng
        }
    }


    // Hàm xử lý tra cứu API (Controller điều phối API service và Formatter)
    private void fetchOnlineDefinition(String word) {
        // Đảm bảo từ không rỗng trước khi gọi API
        if (word == null || word.trim().isEmpty()) {
            Platform.runLater(() -> {
                // Chỉ hiển thị thông báo lỗi này nếu không có định nghĩa cục bộ
                if (!localDictionaryRaw.containsKey(word)) {
                    definitionArea.setText("Từ cần tra cứu online không hợp lệ.");
                } // Nếu có cục bộ, giữ nguyên định nghĩa cục bộ
            });
            return;
        }
        String finalWord = word.trim();

        // Chạy tác vụ mạng trên background thread
        executorService.submit(() -> {
            String formattedApiDefinition; // Chuỗi đã format từ API
            try {
                String rawJsonResponse = apiService.fetchDefinition(finalWord); // Gọi API service

                // Kiểm tra nếu API trả về lỗi "Không tìm thấy"
                if (apiService.isNotFoundResponse(rawJsonResponse)) {
                    formattedApiDefinition = "Không tìm thấy định nghĩa online cho '" + finalWord + "'.";
                } else {
                    // Nếu API trả về OK, format phản hồi JSON
                    formattedApiDefinition = definitionFormatter.formatApiDefinition(rawJsonResponse);
                }

                // Cập nhật UI trên JavaFX Application Thread
                Platform.runLater(() -> {
                    // Kiểm tra nếu đã có định nghĩa cục bộ hiển thị (không phải thông báo chờ API)
                    boolean hasLocalDef = localDictionaryRaw != null && localDictionaryRaw.containsKey(finalWord); // Kiểm tra null cho Map
                    boolean isFetchingMsg = definitionArea.getText().contains("Đang tra cứu online");

                    if (hasLocalDef && !isFetchingMsg) {
                        // Có định nghĩa cục bộ, thêm API vào cuối
                        definitionArea.appendText("\n\n--- Định nghĩa online ---\n" + formattedApiDefinition);
                    } else {
                        // Không có định nghĩa cục bộ, hoặc đang hiển thị thông báo chờ API, ghi đè
                        // (Trường hợp không tìm thấy cục bộ VÀ API cũng không tìm thấy sẽ ghi đè bằng thông báo "Không tìm thấy...")
                        definitionArea.setText(formattedApiDefinition);
                    }
                    // wordDisplayLabel.setText("@" + finalWord); // Label đã được set ở displayWordDefinition
                });

            } catch (IOException e) {
                // Xử lý lỗi mạng hoặc lỗi HTTP non-OK/non-404 từ API service
                System.err.println("Lỗi khi tra cứu online cho '" + finalWord + "': " + e.getMessage());
                Platform.runLater(() -> {
                    String errorMsg = "Lỗi mạng hoặc kết nối khi tra cứu online '" + finalWord + "': " + e.getMessage();
                    // Chỉ hiển thị thông báo lỗi chi tiết này nếu không có định nghĩa cục bộ
                    boolean hasLocalDef = localDictionaryRaw != null && localDictionaryRaw.containsKey(finalWord);
                    if (!hasLocalDef) {
                        definitionArea.setText(errorMsg);
                    } else {
                        // Nếu đã có cục bộ, chỉ thêm thông báo lỗi API vào cuối
                        definitionArea.appendText("\n\n--- Lỗi khi tra cứu online ---\n" + errorMsg);
                    }
                    // wordDisplayLabel.setText("@" + finalWord); // Label đã được set ở displayWordDefinition
                });
            } catch (Exception e) {
                // Xử lý lỗi parsing JSON trong Formatter hoặc lỗi khác
                System.err.println("Lỗi xử lý phản hồi API cho '" + finalWord + "': " + e.getMessage());
                e.printStackTrace(); // In stack trace chi tiết lỗi parsing
                Platform.runLater(() -> {
                    String errorMsg = "Lỗi xử lý dữ liệu online cho '" + finalWord + "': " + e.getMessage();
                    boolean hasLocalDef = localDictionaryRaw != null && localDictionaryRaw.containsKey(finalWord);
                    if (!hasLocalDef) {
                        definitionArea.setText(errorMsg);
                    } else {
                        definitionArea.appendText("\n\n--- Lỗi khi tra cứu online ---\n" + errorMsg);
                    }
                    // wordDisplayLabel.setText("@" + finalWord); // Label đã được set ở displayWordDefinition
                });
            }
        });
    }


    // filterWordList, clearDefinition, showAlert, shutdown vẫn ở đây

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

    // Hàm để đóng ExecutorService khi ứng dụng tắt (quan trọng)
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            System.out.println("Đóng ExecutorService...");
            executorService.shutdown();
        }
        // Thêm shutdown cho các service khác nếu chúng có tài nguyên cần giải phóng
    }
}