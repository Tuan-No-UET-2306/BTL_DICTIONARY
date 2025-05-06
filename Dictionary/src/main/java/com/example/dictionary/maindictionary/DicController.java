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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;      // Import List
import java.util.ArrayList; // Import ArrayList

// Import các class từ cùng package maindictionary
// import com.example.dictionary.maindictionary.DicDataLoader; // Nếu cùng package thì không cần import tường minh
// import com.example.dictionary.maindictionary.DicApiService;
// import com.example.dictionary.maindictionary.DefinitionFormatter;
// import com.example.dictionary.maindictionary.UserWordFileManager; // Static methods, không cần import instance


public class DicController implements Initializable {

    // --- Thành phần UI (View) ---
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
    @FXML
    private Button deleteButton;

    // --- Dữ liệu trong bộ nhớ (Model representation) ---
    private final ObservableList<String> masterWordList = FXCollections.observableArrayList();
    private FilteredList<String> filteredWordList;
    private Map<String, String> localDictionaryRaw; // Từ và định nghĩa thô từ cả 2 file

    // --- Các đối tượng chức năng (Services/Managers) ---
    private DicDataLoader dataLoader; // Để tải dữ liệu ban đầu
    private DicApiService apiService; // Để gọi API online
    private DefinitionFormatter definitionFormatter; // Để định dạng hiển thị
    // UserWordFileManager không cần làm biến thành viên vì các phương thức của nó là static

    // --- Service cho các tác vụ nền (để tránh đơ UI) ---
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // --- Phương thức initialize (Thiết lập ban đầu) ---
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupServices();        // 1. Khởi tạo các đối tượng chức năng
        loadInitialData();      // 2. Tải dữ liệu từ điển ban đầu
        setupListView();        // 3. Setup FilteredList và ListView
        setupSearchField();     // 4. Thêm listener cho searchField và liên kết nút Search
        setupButtonActions();   // 5. Gắn action cho các nút Add/Delete
        setupInitialUIState();  // 6. Cấu hình trạng thái ban đầu UI

        // 7. Ghi chú: ExecutorService cần được shutdown khi ứng dụng tắt.
        //    Thường thì việc này được xử lý ở lớp Main Application.
    }

    // --- Phương thức Setup Helper ---

    private void setupServices() {
        dataLoader = new DicDataLoader("/com/example/dictionary/dic_words.txt");
        apiService = new DicApiService();
        definitionFormatter = new DefinitionFormatter();
    }

    private void loadInitialData() {
        try {
            localDictionaryRaw = dataLoader.loadWordsAndRawDefinitions();
            if (localDictionaryRaw != null) {
                masterWordList.addAll(localDictionaryRaw.keySet());
                FXCollections.sort(masterWordList);
            } else {
                localDictionaryRaw = new HashMap<>(); // Đảm bảo Map không null
            }
        } catch (RuntimeException e) {
            System.err.println("Lỗi tải dữ liệu từ điển: " + e.getMessage());
            showAlert("Lỗi tải dữ liệu", "Không thể tải dữ liệu từ điển ban đầu: " + e.getMessage());
            localDictionaryRaw = new HashMap<>();
            masterWordList.clear();
        }
    }

    private void setupListView() {
        filteredWordList = new FilteredList<>(masterWordList, p -> true);
        wordListView.setItems(filteredWordList);

        wordListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                displayWordDefinition(newValue);
            } else {
                clearDefinition();
            }
        });
    }

    private void setupSearchField() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterWordList(newValue);
        });
        searchField.setOnAction(event -> handleSearchAction()); // Liên kết Enter
    }

    private void setupButtonActions() {
        searchButton.setOnAction(event -> handleSearchAction());
        addButton.setOnAction(event -> handleAddWordAction());
        deleteButton.setOnAction(event -> handleDeleteWordAction());
        backButton.setOnAction(event -> handleBack(event)); // Gắn action cho nút back
    }

    private void setupInitialUIState() {
        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
        definitionArea.setEditable(false);
        deleteButton.setDisable(true); // Ban đầu vô hiệu hóa nút xóa
    }


    // --- Logic xử lý sự kiện UI (@FXML Methods) ---

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

        if (localDictionaryRaw != null && localDictionaryRaw.containsKey(wordToAdd)) {
            showAlert("Thông báo", "Từ '" + wordToAdd + "' đã có trong từ điển.");
            displayWordDefinition(wordToAdd); // Hiển thị định nghĩa nếu đã có
            return;
        }

        // Vô hiệu hóa nút và báo trạng thái đang xử lý
        setButtonState(true, true); // Disable Add và Delete
        definitionArea.appendText("\n-- Đang thêm '" + wordToAdd + "' vào từ điển cục bộ... --");

        // Thực hiện tác vụ thêm trên luồng nền
        executorService.submit(() -> {
            try {
                // 1. Chuẩn bị dữ liệu và gọi lớp quản lý file để ghi
                String defaultRawDefinition = "**unclassified** No definition provided yet.\\\\";
                String lineToAppend = wordToAdd + " " + defaultRawDefinition;
                UserWordFileManager.appendLineToFile(lineToAppend); // Ghi vào file

                // 2. Cập nhật UI và bộ nhớ trên luồng JavaFX Application Thread
                Platform.runLater(() -> handleAddWordSuccess(wordToAdd, defaultRawDefinition));

            } catch (IOException e) {
                // 3. Xử lý lỗi và cập nhật UI trên luồng JavaFX Application Thread
                Platform.runLater(() -> handleAddWordError(wordToAdd, e.getMessage()));
            } catch (Exception e) {
                // 4. Xử lý lỗi không xác định
                System.err.println("Lỗi không xác định khi thêm từ: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> handleAddWordError(wordToAdd, "Lỗi không xác định: " + e.getMessage()));
            }
        });
    }


    @FXML
    private void handleDeleteWordAction() {
        String wordToDelete = wordDisplayLabel.getText().replace("@", "").trim();

        if (wordToDelete.isEmpty()) {
            showAlert("Lỗi", "Không có từ nào đang hiển thị để xóa.");
            setButtonState(false, true); // Vô hiệu hóa lại nút xóa nếu hiển thị không hợp lệ
            return;
        }

        if (localDictionaryRaw == null || !localDictionaryRaw.containsKey(wordToDelete)) {
            showAlert("Thông báo", "Từ '" + wordToDelete + "' không có trong từ điển cục bộ để xóa.");
            setButtonState(false, true); // Vô hiệu hóa nút xóa
            return;
        }

        // Yêu cầu xác nhận từ người dùng
        Optional<ButtonType> result = showConfirmationDialog("Xác nhận xóa", "Bạn có chắc chắn muốn xóa từ '" + wordToDelete + "' khỏi từ điển cục bộ không?");
        if (!result.isPresent() || result.get() != ButtonType.OK) {
            return; // Người dùng hủy
        }

        // Vô hiệu hóa nút và báo trạng thái đang xử lý
        setButtonState(true, true); // Disable Add và Delete
        definitionArea.appendText("\n-- Đang xóa '" + wordToDelete + "' khỏi từ điển cục bộ... --");

        // Thực hiện tác vụ xóa trên luồng nền
        executorService.submit(() -> {
            try {
                // 1. Gọi lớp quản lý file để xóa
                boolean deletedFromFile = UserWordFileManager.deleteWordFromFile(wordToDelete);

                // 2. Cập nhật UI và bộ nhớ trên luồng JavaFX Application Thread
                Platform.runLater(() -> handleDeleteWordResult(wordToDelete, deletedFromFile));

            } catch (IOException e) {
                // 3. Xử lý lỗi và cập nhật UI trên luồng JavaFX Application Thread
                Platform.runLater(() -> handleDeleteWordError(wordToDelete, e.getMessage()));
            } catch (Exception e) {
                // 4. Xử lý lỗi không xác định
                System.err.println("Lỗi không xác định khi xóa từ: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> handleDeleteWordError(wordToDelete, "Lỗi không xác định: " + e.getMessage()));
            }
        });
    }

    // --- Logic hiển thị định nghĩa (phương thức điều phối) ---
    private void displayWordDefinition(String word) {
        if (word == null || word.trim().isEmpty()) {
            clearDefinition();
            return;
        }
        String cleanedWord = word.trim();

        // Cập nhật UI ban đầu
        wordDisplayLabel.setText("@" + cleanedWord);
        definitionArea.setText("Đang xử lý '" + cleanedWord + "'...");
        setButtonState(false, false); // Ban đầu bật cả hai nút, sẽ điều chỉnh sau

        // 1. Thử tra cứu định nghĩa cục bộ (từ cả file gốc và file thêm)
        String rawLocalDef = localDictionaryRaw != null ? localDictionaryRaw.get(cleanedWord) : null;

        if (rawLocalDef != null) {
            // Nếu tìm thấy cục bộ, format và hiển thị
            String formattedDef = definitionFormatter.formatLocalDefinition(cleanedWord, rawLocalDef);
            Platform.runLater(() -> {
                definitionArea.setText(formattedDef);
                setButtonState(false, true); // Chỉ bật nút Delete vì từ này có trong cục bộ
            });
        } else {
            // Nếu không tìm thấy cục bộ, thông báo, và chuẩn bị gọi API
            Platform.runLater(() -> {
                definitionArea.setText("Không tìm thấy định nghĩa cục bộ cho '" + cleanedWord + "'. Đang tra cứu online...");
                setButtonState(true, false); // Bật nút Add, vô hiệu hóa nút Delete
            });
            // 2. Gọi API (trên luồng nền)
            fetchOnlineDefinition(cleanedWord);
        }
    }


    // --- Các phương thức Helper (Private Methods) ---

    // Helper để bật/tắt các nút Add và Delete
    private void setButtonState(boolean disableAdd, boolean disableDelete) {
        addButton.setDisable(disableAdd);
        deleteButton.setDisable(disableDelete);
    }

    // Helper để xử lý kết quả thêm từ thành công trên luồng UI
    private void handleAddWordSuccess(String word, String rawDefinition) {
        if (localDictionaryRaw != null) {
            // Cập nhật bộ nhớ
            localDictionaryRaw.put(word, rawDefinition);
            masterWordList.add(word); // Thêm vào list hiển thị
            FXCollections.sort(masterWordList); // Sắp xếp lại list
        }

        // Cập nhật UI
        definitionArea.appendText("\n-- Đã thêm từ '" + word + "' vào từ điển cục bộ. --");
        System.out.println("Added word '" + word + "' to local dictionary.");

        // Khôi phục trạng thái nút (bật Add, bật Delete cho từ vừa thêm)
        setButtonState(false, true); // Từ vừa thêm CÓ trong local, nên bật nút Delete
        // Tự động hiển thị định nghĩa của từ vừa thêm
        // displayWordDefinition(word); // Có thể gọi lại hàm này nếu muốn load lại definition sau khi thêm
    }

    // Helper để xử lý lỗi khi thêm từ trên luồng UI
    private void handleAddWordError(String word, String errorMessage) {
        System.err.println("Lỗi khi thêm từ '" + word + "': " + errorMessage);
        showAlert("Lỗi", "Không thể thêm từ '" + word + "' vào từ điển cục bộ: " + errorMessage);

        // Khôi phục trạng thái nút
        setButtonState(false, localDictionaryRaw == null || !localDictionaryRaw.containsKey(word));
    }


    // Helper để xử lý kết quả xóa từ trên luồng UI
    private void handleDeleteWordResult(String word, boolean deletedFromFile) {
        if (deletedFromFile) {
            // Cập nhật bộ nhớ
            if (localDictionaryRaw != null) {
                localDictionaryRaw.remove(word);
            }
            masterWordList.remove(word); // Xóa khỏi list hiển thị

            // Cập nhật UI
            wordDisplayLabel.setText("@" + word); // Vẫn hiển thị từ đã xóa trên label
            definitionArea.setText("Đã xóa từ '" + word + "' khỏi từ điển cục bộ.");
            System.out.println("Updated in-memory dictionary after deletion for '" + word + "'.");
            setButtonState(false, false); // Bật lại nút Add, vô hiệu hóa nút Delete
            clearDefinition(); // Xóa nội dung hiển thị định nghĩa sau khi xóa
        } else {
            // Trường hợp không tìm thấy từ trong file user_added_words.txt để xóa (có thể từ gốc)
            definitionArea.appendText("\n-- Từ '" + word + "' không được tìm thấy trong file có thể sửa đổi (user added file) để xóa. --");
            System.err.println("Deletion attempt failed for word: " + word + " (Not found in user added file)");
            // Nút Delete vẫn disabled từ lúc bắt đầu action, nút Add đã được bật lại.
            setButtonState(false, true); // Bật nút Add, vô hiệu hóa nút Delete
        }

        // Khôi phục trạng thái nút (nút Add luôn được bật lại)
        // Nút Delete đã được set đúng trạng thái bên trong logic.
    }

    // Helper để xử lý lỗi khi xóa từ trên luồng UI
    private void handleDeleteWordError(String word, String errorMessage) {
        System.err.println("Lỗi khi xóa từ '" + word + "': " + errorMessage);
        showAlert("Lỗi", "Không thể xóa từ '" + word + "' khỏi từ điển cục bộ: " + errorMessage);

        // Khôi phục trạng thái nút
        setButtonState(false, localDictionaryRaw == null || !localDictionaryRaw.containsKey(word));
    }


    // Helper để xử lý tra cứu API trên luồng nền
    private void fetchOnlineDefinition(String word) {
        if (word == null || word.trim().isEmpty()) {
            // Logic kiểm tra đầu vào rỗng đã có ở displayWordDefinition, không cần lặp lại
            return;
        }
        String finalWord = word.trim(); // Sử dụng biến final để dùng trong lambda expression

        executorService.submit(() -> {
            String rawJsonResponse = null;
            try {
                // 1. Gọi API trên luồng nền
                rawJsonResponse = apiService.fetchDefinition(finalWord);

                // 2. Định dạng kết quả trên luồng nền (nếu formatter không thao tác với UI)
                //    Lưu ý: Nếu DefinitionFormatter rất nặng, có thể cần tối ưu.
                String formattedApiDefinition;
                if (apiService.isNotFoundResponse(rawJsonResponse)) {
                    formattedApiDefinition = "Không tìm thấy định nghĩa online cho '" + finalWord + "'.";
                } else {
                    formattedApiDefinition = definitionFormatter.formatApiDefinition(rawJsonResponse);
                }

                // 3. Cập nhật UI trên luồng JavaFX Application Thread
                Platform.runLater(() -> handleOnlineFetchSuccess(finalWord, formattedApiDefinition));

            } catch (IOException e) {
                // 4. Xử lý lỗi IO (mạng, kết nối) trên luồng UI
                Platform.runLater(() -> handleOnlineFetchError(finalWord, e.getMessage()));
            } catch (Exception e) {
                // 5. Xử lý lỗi khác (ví dụ: lỗi phân tích JSON nếu formatApiDefinition ném ngoại lệ) trên luồng UI
                System.err.println("Lỗi xử lý phản hồi API cho '" + finalWord + "': " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> handleOnlineFetchError(finalWord, "Lỗi xử lý dữ liệu online: " + e.getMessage()));
            }
        });
    }

    // Helper để xử lý kết quả tra cứu online thành công (bao gồm cả không tìm thấy) trên luồng UI
    private void handleOnlineFetchSuccess(String word, String formattedDefinition) {
        boolean hasLocalDef = localDictionaryRaw != null && localDictionaryRaw.containsKey(word);
        // Kiểm tra lại xem người dùng có đang xem định nghĩa của từ khác không
        // (trường hợp người dùng gõ/chọn từ khác trong lúc đang fetch online)
        if (!wordDisplayLabel.getText().replace("@", "").trim().equalsIgnoreCase(word)) {
            System.out.println("Ignoring stale online result for '" + word + "' as current word displayed is different.");
            return; // Bỏ qua kết quả cũ
        }

        if (hasLocalDef) {
            // Nếu đã có định nghĩa cục bộ, nối thêm định nghĩa online
            definitionArea.appendText("\n\n--- Định nghĩa online ---\n" + formattedDefinition);
        } else {
            // Nếu không có định nghĩa cục bộ, hiển thị định nghĩa online (ghi đè thông báo "Đang tra cứu...")
            definitionArea.setText(formattedDefinition);
        }
        // Trạng thái nút đã được set đúng ở displayWordDefinition
        // (Add bật, Delete tắt khi không có local def)
    }

    // Helper để xử lý lỗi tra cứu online trên luồng UI
    private void handleOnlineFetchError(String word, String errorMessage) {
        System.err.println("Lỗi tra cứu online cho '" + word + "': " + errorMessage);

        // Kiểm tra lại xem người dùng có đang xem định nghĩa của từ khác không
        if (!wordDisplayLabel.getText().replace("@", "").trim().equalsIgnoreCase(word)) {
            System.out.println("Ignoring stale online error for '" + word + "' as current word displayed is different.");
            return; // Bỏ qua lỗi cũ
        }

        String errorMsg = "Lỗi mạng hoặc kết nối khi tra cứu online '" + word + "': " + errorMessage;
        boolean hasLocalDef = localDictionaryRaw != null && localDictionaryRaw.containsKey(word);

        if (!hasLocalDef) {
            // Nếu không có định nghĩa cục bộ, hiển thị lỗi online (ghi đè thông báo "Đang tra cứu...")
            definitionArea.setText(errorMsg);
            setButtonState(false, false); // Bật Add, tắt Delete (đã set ở displayWordDefinition) -> Không cần set lại ở đây trừ khi logic phức tạp hơn
        } else {
            // Nếu đã có định nghĩa cục bộ, nối thêm thông báo lỗi online
            definitionArea.appendText("\n\n--- Lỗi khi tra cứu online ---\n" + errorMsg);
            // Trạng thái nút đã được set đúng ở displayWordDefinition (Add tắt, Delete bật)
        }
    }


    // Helper để lọc danh sách từ trong ListView
    private void filterWordList(String filter) {
        if (filter == null || filter.isEmpty()) {
            filteredWordList.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = filter.toLowerCase();
            filteredWordList.setPredicate(word -> word.toLowerCase().startsWith(lowerCaseFilter));
        }

        // Cuộn đến từ đầu tiên nếu có kết quả lọc
        if (!filteredWordList.isEmpty()) {
            Platform.runLater(() -> {
                // Chỉ cuộn, không tự động hiển thị định nghĩa khi gõ
                wordListView.scrollTo(filteredWordList.get(0));
            });
        } else {
            // Xóa hiển thị định nghĩa nếu danh sách lọc trống
            Platform.runLater(() -> {
                wordListView.getSelectionModel().clearSelection();
                clearDefinition();
            });
        }
    }


    // Helper để xóa nội dung vùng định nghĩa và label từ
    private void clearDefinition() {
        wordDisplayLabel.setText("");
        definitionArea.clear();
        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
        deleteButton.setDisable(true); // Luôn vô hiệu hóa nút xóa khi không có từ nào hiển thị
        addButton.setDisable(false); // Luôn bật nút thêm (có thể thêm bất kỳ từ nào)
    }


    // Helper để hiển thị thông báo Alert
    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert.AlertType alertType = title != null && title.toLowerCase().contains("lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // Helper để hiển thị hộp thoại xác nhận
    private Optional<ButtonType> showConfirmationDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait();
    }

    // Hàm để đóng ExecutorService khi ứng dụng tắt (cần gọi từ lớp Main)
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            System.out.println("Đóng ExecutorService cho DicController...");
            executorService.shutdownNow(); // Dùng shutdownNow để dừng ngay các tác vụ đang chạy
        }
    }
}