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
import java.net.URL;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional; // <-- Import cần thiết cho Confirmation Dialog
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Import các class từ cùng package maindictionary
// import com.example.dictionary.maindictionary.DicDataLoader;
// import com.example.dictionary.maindictionary.DicApiService;
// import com.example.dictionary.maindictionary.DefinitionFormatter;

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

    @FXML
    private Button addButton;

    @FXML // <-- @FXML field cho nút Delete
    private Button deleteButton;


    // --- Biến lưu trữ dữ liệu ---
    private final ObservableList<String> masterWordList = FXCollections.observableArrayList();
    private FilteredList<String> filteredWordList;

    private Map<String, String> localDictionaryRaw;


    // --- Các đối tượng chức năng (Services) ---
    private DicDataLoader dataLoader;
    private DicApiService apiService;
    private DefinitionFormatter definitionFormatter;


    // --- Service cho các tác vụ nền ---
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // --- Phương thức initialize ---
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Khởi tạo các đối tượng chức năng
        dataLoader = new DicDataLoader("/com/example/dictionary/dic_words.txt");
        apiService = new DicApiService();
        definitionFormatter = new DefinitionFormatter();

        // 2. Tải dữ liệu từ điển ban đầu
        loadDictionaryWords();

        // 3. Setup FilteredList
        filteredWordList = new FilteredList<>(masterWordList, p -> true);
        wordListView.setItems(filteredWordList);

        // 4. Thêm listener cho searchField
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterWordList(newValue);
        });

        // 5. Thêm listener cho ListView
        wordListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                displayWordDefinition(newValue); // Gọi phương thức hiển thị chính
            } else {
                clearDefinition();
            }
        });

        // 6. Cấu hình ban đầu
        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
        definitionArea.setEditable(false);

        // Cấu hình ban đầu cho nút Delete (vô hiệu hóa)
        deleteButton.setDisable(true); // <-- Vô hiệu hóa nút Delete ban đầu

        // 7. Liên kết Enter và nút Search
        searchField.setOnAction(event -> handleSearchAction());
        searchButton.setOnAction(event -> handleSearchAction());

        // 8. Liên kết nút Add
        addButton.setOnAction(event -> handleAddWordAction());

        // 9. Liên kết nút Delete // <-- Thêm liên kết cho nút Delete
        deleteButton.setOnAction(event -> handleDeleteWordAction());
    }

    // --- Logic xử lý sự kiện ---

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
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
            displayWordDefinition(searchTerm);
        } else {
            wordListView.getSelectionModel().clearSelection();
            clearDefinition();
        }
    }

    @FXML
    private void handleAddWordAction() {
        String wordToAdd = searchField.getText().trim();

        if (wordToAdd.isEmpty()) {
            showAlert("Lỗi", "Không có từ nào để thêm.");
            return;
        }

        // Kiểm tra lại lần nữa xem từ đã có trong danh sách cục bộ chưa
        if (localDictionaryRaw != null && localDictionaryRaw.containsKey(wordToAdd)) {
            showAlert("Thông báo", "Từ '" + wordToAdd + "' đã có trong từ điển cục bộ.");
            // deleteButton.setDisable(false); // Có thể enable nút Delete nếu hiển thị từ đã có
            displayWordDefinition(wordToAdd); // Hiển thị định nghĩa cục bộ
            return;
        }

        // Vô hiệu hóa nút Add và Delete để tránh thao tác trong khi ghi file
        addButton.setDisable(true);
        deleteButton.setDisable(true); // <-- Vô hiệu hóa nút Delete khi thêm
        definitionArea.appendText("\n-- Đang thêm '" + wordToAdd + "' vào từ điển cục bộ... --");


        executorService.submit(() -> {
            try {
                String defaultRawDefinition = "**unclassified** No definition provided yet.\\\\";
                dataLoader.appendWordToFile(wordToAdd, defaultRawDefinition);

                Platform.runLater(() -> {
                    // 1. Cập nhật Map cục bộ
                    if (localDictionaryRaw != null) {
                        localDictionaryRaw.put(wordToAdd, defaultRawDefinition);
                    }

                    // 2. Cập nhật ObservableList cho ListView và sắp xếp
                    masterWordList.add(wordToAdd);
                    FXCollections.sort(masterWordList);

                    // FilteredList sẽ tự cập nhật

                    // 3. Cập nhật TextArea để báo đã thêm thành công
                    if (localDictionaryRaw != null && localDictionaryRaw.containsKey(wordToAdd)) { // Kiểm tra lại sau khi put
                        definitionArea.appendText("\n-- Đã thêm từ '" + wordToAdd + "' vào từ điển cục bộ. --");
                        // displayWordDefinition(wordToAdd); // Có thể gọi lại để hiển thị định nghĩa mặc định
                    } else {
                        definitionArea.appendText("\n-- Đã cố gắng thêm từ '" + wordToAdd + "', nhưng dữ liệu trong bộ nhớ chưa được cập nhật hoàn toàn. --");
                    }

                    System.out.println("Added word '" + wordToAdd + "' to local dictionary.");

                    // Bật lại nút Add và Delete (Delete có thể sẽ bị disable ngay sau đó nếu TextArea được cập nhật)
                    addButton.setDisable(false);
                    // displayWordDefinition(wordToAdd); // <-- Gọi lại displayWordDefinition để xử lý trạng thái nút Delete
                    // Hoặc đơn giản:
                    if (localDictionaryRaw != null && localDictionaryRaw.containsKey(wordToAdd)) {
                        deleteButton.setDisable(false); // Bật nút Delete nếu từ vừa thêm đã có trong map
                    } else {
                        deleteButton.setDisable(true); // Nếu không vì lý do nào đó
                    }
                });

            } catch (IOException e) {
                System.err.println("Lỗi khi thêm từ vào file: " + e.getMessage());
                Platform.runLater(() -> {
                    showAlert("Lỗi", "Không thể thêm từ '" + wordToAdd + "' vào từ điển cục bộ: " + e.getMessage());
                    addButton.setDisable(false); // Bật lại nút Add khi lỗi
                    deleteButton.setDisable(false); // Bật lại nút Delete khi lỗi
                });
            } catch (Exception e) {
                System.err.println("Lỗi không xác định khi thêm từ: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Lỗi", "Có lỗi xảy ra khi thêm từ '" + wordToAdd + "': " + e.getMessage());
                    addButton.setDisable(false); // Bật lại nút Add khi lỗi
                    deleteButton.setDisable(false); // Bật lại nút Delete khi lỗi
                });
            }
        });
    }

    @FXML // <-- @FXML cho handler nút Delete
    private void handleDeleteWordAction() { // Phương thức xử lý khi nhấn nút "Delete Word"
        String wordToDelete = searchField.getText().trim(); // Lấy từ từ searchField (hoặc wordDisplayLabel)

        if (wordToDelete.isEmpty()) {
            showAlert("Lỗi", "Không có từ nào để xóa.");
            return;
        }

        // Xác nhận từ người dùng trước khi xóa (quan trọng!)
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận xóa");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("Bạn có chắc chắn muốn xóa từ '" + wordToDelete + "' khỏi từ điển cục bộ không?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() != ButtonType.OK) {
            // Người dùng hủy bỏ thao tác
            return;
        }

        // Kiểm tra xem từ này có thực sự trong dữ liệu cục bộ không
        if (localDictionaryRaw == null || !localDictionaryRaw.containsKey(wordToDelete)) {
            showAlert("Thông báo", "Từ '" + wordToDelete + "' không có trong từ điển cục bộ để xóa.");
            deleteButton.setDisable(true); // Vô hiệu hóa nút nếu không có
            return;
        }

        // Vô hiệu hóa nút Add và Delete để tránh thao tác trong khi xóa file
        addButton.setDisable(true);
        deleteButton.setDisable(true); // <-- Vô hiệu hóa nút Delete khi xóa
        definitionArea.appendText("\n-- Đang xóa '" + wordToDelete + "' khỏi từ điển cục bộ... --");

        // Xóa từ khỏi file ở background thread
        executorService.submit(() -> {
            try {
                boolean deletedFromFile = dataLoader.deleteWordFromFile(wordToDelete); // Gọi DataLoader để xóa file

                Platform.runLater(() -> {
                    if (deletedFromFile) {
                        // 1. Cập nhật Map cục bộ
                        if (localDictionaryRaw != null) {
                            localDictionaryRaw.remove(wordToDelete);
                        }

                        // 2. Cập nhật ObservableList cho ListView
                        masterWordList.remove(wordToDelete);
                        // FilteredList sẽ tự cập nhật

                        // 3. Xóa nội dung vùng định nghĩa cho từ vừa xóa
                        // clearDefinition(); // Cách đơn giản là xóa sạch

                        // Hoặc hiển thị thông báo đã xóa và từ đó
                        wordDisplayLabel.setText("@" + wordToDelete); // Vẫn hiển thị từ đã xóa
                        definitionArea.setText("Đã xóa từ '" + wordToDelete + "' khỏi từ điển cục bộ.");

                        System.out.println("Deleted word '" + wordToDelete + "' from local dictionary.");

                    } else {
                        // Trường hợp không xóa được file (file không tồn tại hoặc từ không có trong file)
                        // Logic này đã được kiểm tra ở trên và trong DataLoader, nên có thể không cần thiết ở đây
                        definitionArea.appendText("\n-- Từ '" + wordToDelete + "' không được tìm thấy trong file cục bộ hoặc có lỗi khi xóa. --");
                        System.err.println("Delete failed for word: " + wordToDelete);
                    }

                    // Bật lại nút Add và Delete
                    addButton.setDisable(false);
                    // deleteButton.setDisable(false); // Nút Delete sẽ bị disable ngay sau khi definitionArea được cập nhật (vì từ không còn trong map cục bộ)
                    // Gọi displayWordDefinition() cho từ vừa xóa để cập nhật đúng trạng thái nút Disable
                    displayWordDefinition(wordToDelete); // Sẽ dẫn đến không tìm thấy cục bộ -> disable nút Delete
                });

            } catch (IOException e) {
                System.err.println("Lỗi khi xóa từ khỏi file: " + e.getMessage());
                Platform.runLater(() -> {
                    showAlert("Lỗi", "Không thể xóa từ '" + wordToDelete + "' khỏi từ điển cục bộ: " + e.getMessage());
                    addButton.setDisable(false); // Bật lại nút Add khi lỗi
                    deleteButton.setDisable(false); // Bật lại nút Delete khi lỗi
                });
            } catch (Exception e) { // Bắt các lỗi khác có thể xảy ra
                System.err.println("Lỗi không xác định khi xóa từ: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Lỗi", "Có lỗi xảy ra khi xóa từ '" + wordToDelete + "': " + e.getMessage());
                    addButton.setDisable(false); // Bật lại nút Add khi lỗi
                    deleteButton.setDisable(false); // Bật lại nút Delete khi lỗi
                });
            }
        });
    }


    // --- Phương thức hiển thị định nghĩa chính (Controller điều phối) ---
    private void displayWordDefinition(String word) {
        if (word == null || word.trim().isEmpty()) {
            clearDefinition(); // clearDefinition cũng sẽ disable nút Delete
            return;
        }
        String cleanedWord = word.trim();

        wordDisplayLabel.setText("@" + cleanedWord);
        definitionArea.setText("Đang xử lý '" + cleanedWord + "'...");

        // 1. Thử tra cứu định nghĩa cục bộ
        String rawLocalDef = localDictionaryRaw != null ? localDictionaryRaw.get(cleanedWord) : null;

        if (rawLocalDef != null) {
            // Nếu tìm thấy cục bộ, format và hiển thị
            String formattedDef = definitionFormatter.formatLocalDefinition(cleanedWord, rawLocalDef);
            Platform.runLater(() -> {
                definitionArea.setText(formattedDef);
                deleteButton.setDisable(false); // <-- Kích hoạt nút Delete khi tìm thấy cục bộ
            });
        } else {
            // Nếu không tìm thấy cục bộ, thông báo, và chuẩn bị gọi API
            Platform.runLater(() -> {
                definitionArea.setText("Không tìm thấy định nghĩa cục bộ cho '" + cleanedWord + "'. Đang tra cứu online...");
                deleteButton.setDisable(true); // <-- Vô hiệu hóa nút Delete khi KHÔNG tìm thấy cục bộ
            });
            // 2. Gọi API
            fetchOnlineDefinition(cleanedWord);
        }
    }


    // --- Các hàm helper ---

    // Hàm tải dữ liệu từ điển từ file dic_words.txt trong resources và file thêm
    private void loadDictionaryWords() {
        try {
            localDictionaryRaw = dataLoader.loadWordsAndRawDefinitions();
            if (localDictionaryRaw != null) {
                masterWordList.addAll(localDictionaryRaw.keySet());
                FXCollections.sort(masterWordList);
            } else {
                localDictionaryRaw = new HashMap<>();
            }

        } catch (RuntimeException e) {
            System.err.println("Lỗi tải dữ liệu từ điển: " + e.getMessage());
            showAlert("Lỗi tải dữ liệu", "Không thể tải dữ liệu từ điển ban đầu: " + e.getMessage());
            localDictionaryRaw = new HashMap<>();
            masterWordList.clear();
        }
    }


    // Hàm xử lý tra cứu API
    private void fetchOnlineDefinition(String word) {
        if (word == null || word.trim().isEmpty()) {
            Platform.runLater(() -> {
                if (localDictionaryRaw == null || !localDictionaryRaw.containsKey(word)) {
                    definitionArea.setText("Từ cần tra cứu online không hợp lệ.");
                }
            });
            return;
        }
        String finalWord = word.trim();

        executorService.submit(() -> {
            String formattedApiDefinition;
            boolean apiFound = false;

            try {
                String rawJsonResponse = apiService.fetchDefinition(finalWord);

                if (apiService.isNotFoundResponse(rawJsonResponse)) {
                    formattedApiDefinition = "Không tìm thấy định nghĩa online cho '" + finalWord + "'.";
                    apiFound = false;
                } else {
                    formattedApiDefinition = definitionFormatter.formatApiDefinition(rawJsonResponse);
                    apiFound = true;
                }

                Platform.runLater(() -> {
                    boolean hasLocalDef = localDictionaryRaw != null && localDictionaryRaw.containsKey(finalWord);
                    boolean isFetchingMsg = definitionArea.getText().contains("Đang tra cứu online");

                    if (hasLocalDef && !isFetchingMsg) {
                        definitionArea.appendText("\n\n--- Định nghĩa online ---\n" + formattedApiDefinition);
                    } else {
                        definitionArea.setText(formattedApiDefinition);
                    }
                    // wordDisplayLabel đã được set ở displayWordDefinition
                    // Trạng thái nút Delete đã được set ở displayWordDefinition
                });

            } catch (IOException e) {
                System.err.println("Lỗi khi tra cứu online cho '" + finalWord + "': " + e.getMessage());
                Platform.runLater(() -> {
                    String errorMsg = "Lỗi mạng hoặc kết nối khi tra cứu online '" + finalWord + "': " + e.getMessage();
                    boolean hasLocalDef = localDictionaryRaw != null && localDictionaryRaw.containsKey(finalWord);
                    if (!hasLocalDef) {
                        definitionArea.setText(errorMsg);
                    } else {
                        definitionArea.appendText("\n\n--- Lỗi khi tra cứu online ---\n" + errorMsg);
                    }
                });
            } catch (Exception e) {
                System.err.println("Lỗi xử lý phản hồi API cho '" + finalWord + "': " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    String errorMsg = "Lỗi xử lý dữ liệu online cho '" + finalWord + "': " + e.getMessage();
                    boolean hasLocalDef = localDictionaryRaw != null && localDictionaryRaw.containsKey(finalWord);
                    if (!hasLocalDef) {
                        definitionArea.setText(errorMsg);
                    } else {
                        definitionArea.appendText("\n\n--- Lỗi khi tra cứu online ---\n" + errorMsg);
                    }
                });
            }
        });
    }


    // Hàm lọc danh sách từ
    private void filterWordList(String filter) {
        if (filter == null || filter.isEmpty()) {
            filteredWordList.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = filter.toLowerCase();
            filteredWordList.setPredicate(word -> word.toLowerCase().startsWith(lowerCaseFilter));
        }

        if (!filteredWordList.isEmpty()) {
            Platform.runLater(() -> {
                wordListView.scrollTo(filteredWordList.get(0));
                // displayWordDefinition(filteredList.get(0)); // Tùy chọn: Hiển thị định nghĩa từ đầu tiên khi lọc
            });
        } else {
            Platform.runLater(() -> {
                wordListView.getSelectionModel().clearSelection();
                clearDefinition(); // clearDefinition sẽ disable nút Delete
            });
        }
    }

    // Hàm xóa nội dung vùng định nghĩa và label từ
    private void clearDefinition() {
        wordDisplayLabel.setText("");
        definitionArea.clear();
        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
        deleteButton.setDisable(true); // <-- Vô hiệu hóa nút Delete khi xóa định nghĩa
    }

    // Hàm hiển thị thông báo (thường dùng cho lỗi)
    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            // Sử dụng Alert.AlertType.INFORMATION cho thông báo không phải lỗi nghiêm trọng
            Alert.AlertType alertType = title.contains("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // Hàm để đóng ExecutorService khi ứng dụng tắt
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            System.out.println("Đóng ExecutorService...");
            executorService.shutdown();
        }
    }
}