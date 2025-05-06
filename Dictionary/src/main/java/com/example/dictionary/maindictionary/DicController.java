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
import javafx.scene.control.Alert.AlertType;
import java.util.Set; // <-- Import Set
import java.util.HashSet; // <-- Import HashSet

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
    // Lưu trữ định nghĩa thô từ cả hai nguồn (file gốc và file thêm)
    private Map<String, String> localDictionaryRaw;
    // TẬP HỢP CÁC TỪ CÓ TRONG FILE NGƯỜI DÙNG THÊM
    private Set<String> userAddedWordsSet = new HashSet<>(); // <-- BIẾN MỚI để lưu các từ do người dùng thêm


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
        // 1. Khởi tạo các đối tượng chức năng
        setupServices();

        // 2. Tải dữ liệu từ điển ban đầu trên luồng nền
        loadInitialData(); // <-- Đã sửa để xử lý DictionaryLoadResult

        // 3. Setup FilteredList (ban đầu có thể trống, sẽ được populate sau khi tải data)
        filteredWordList = new FilteredList<>(masterWordList, p -> true);
        wordListView.setItems(filteredWordList);

        // 4. Thêm listener cho searchField
        setupSearchFieldListener();

        // 5. Thêm listener cho ListView
        setupListViewListener();

        // 6. Cấu hình ban đầu UI
        setupInitialUIState();

        // 7. Liên kết các nút và phím
        setupButtonActions();
    }

    // --- Phương thức Setup Helper ---

    private void setupServices() {
        // Đường dẫn đến file từ điển gốc trong resources
        dataLoader = new DicDataLoader("/com/example/dictionary/dic_words.txt");
        apiService = new DicApiService();
        definitionFormatter = new DefinitionFormatter();
        // UserWordFileManager không cần khởi tạo instance
    }

    private void loadInitialData() {
        // Hiển thị trạng thái đang tải trên UI
        Platform.runLater(() -> {
            wordDisplayLabel.setText("");
            definitionArea.setText("Đang tải dữ liệu từ điển...");
            definitionArea.setPromptText(""); // Xóa prompt text khi có nội dung
            setButtonState(true, true); // Vô hiệu hóa nút Add/Delete trong khi tải
        });

        // Thực hiện tải dữ liệu trên luồng nền
        executorService.submit(() -> {
            try {
                // Gọi DataLoader để tải dữ liệu và tách biệt từ người dùng thêm
                DictionaryLoadResult loadResult = dataLoader.loadWordsAndRawDefinitions(); // <-- Gọi method mới
                localDictionaryRaw = loadResult.getAllLocalWords();
                userAddedWordsSet = loadResult.getUserAddedWords(); // <-- Lưu set các từ người dùng thêm

                // Cập nhật ObservableList và UI trên luồng JavaFX Application Thread
                Platform.runLater(() -> {
                    masterWordList.addAll(localDictionaryRaw.keySet());
                    FXCollections.sort(masterWordList);
                    System.out.println("Dictionary data loaded. Total words: " + localDictionaryRaw.size() + ", User-added words: " + userAddedWordsSet.size());

                    definitionArea.setText("Dữ liệu từ điển đã tải xong. Sẵn sàng tra cứu.");
                    definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
                    // Sau khi tải xong, nút Add luôn bật, Delete chỉ bật khi có từ hiển thị và là user-added
                    setButtonState(false, true); // Add bật, Delete tắt ban đầu
                });

            } catch (RuntimeException e) { // DataLoader ném RuntimeException khi lỗi nghiêm trọng
                System.err.println("Lỗi tải dữ liệu từ điển: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Lỗi tải dữ liệu", "Không thể tải dữ liệu từ điển ban đầu: " + e.getMessage());
                    localDictionaryRaw = new HashMap<>(); // Đảm bảo Map không null
                    userAddedWordsSet = new HashSet<>(); // Đảm bảo Set không null
                    masterWordList.clear(); // Xóa danh sách nếu tải lỗi
                    definitionArea.setText("Lỗi tải dữ liệu từ điển.");
                    definitionArea.setPromptText("");
                    setButtonState(false, true); // Add bật, Delete tắt
                });
            }
        });
    }

    private void setupSearchFieldListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterWordList(newValue);
        });
        searchField.setOnAction(event -> handleSearchAction()); // Liên kết Enter
    }

    private void setupListViewListener() {
        wordListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                displayWordDefinition(newValue);
            } else {
                clearDefinition();
            }
        });
    }

    private void setupButtonActions() {
        searchButton.setOnAction(event -> handleSearchAction());
        addButton.setOnAction(event -> handleAddWordAction());
        deleteButton.setOnAction(event -> handleDeleteWordAction());
        backButton.setOnAction(event -> handleBack(event)); // Gắn action cho nút back
    }

    private void setupInitialUIState() {
        definitionArea.setPromptText("Đang tải dữ liệu..."); // Text ban đầu trước khi tải
        definitionArea.setEditable(false);
        setButtonState(true, true); // Ban đầu vô hiệu hóa cả hai nút cho đến khi tải xong
    }


    // --- Logic xử lý sự kiện UI (@FXML Methods) ---

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            // Đường dẫn này giả định main.fxml nằm trong thư mục resources/com/example/dictionary/
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
            clearDefinition(); // Xóa hiển thị nếu ô tìm kiếm rỗng
        }
    }

    @FXML
    private void handleAddWordAction() {
        String wordToAdd = searchField.getText().trim();

        if (wordToAdd.isEmpty()) {
            showAlert("Lỗi", "Không có từ nào để thêm.");
            return;
        }

        // Kiểm tra xem từ đã có trong bộ nhớ cục bộ (từ gốc hoặc đã thêm) chưa
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
                // Định nghĩa mặc định cho từ mới thêm
                String defaultRawDefinition = "**unclassified** No definition provided yet.\\\\";
                String lineToAppend = wordToAdd + " " + defaultRawDefinition;
                UserWordFileManager.appendLineToFile(lineToAppend); // Ghi vào file người dùng thêm

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

        // Kiểm tra các điều kiện cơ bản trước khi xóa
        if (wordToDelete.isEmpty()) {
            showAlert("Lỗi", "Không có từ nào đang hiển thị để xóa.");
            setButtonState(false, true); // Add bật, Delete tắt
            return;
        }

        // KIỂM TRA XEM TỪ CÓ PHẢI LÀ TỪ NGƯỜI DÙNG THÊM KHÔNG
        if (userAddedWordsSet == null || !userAddedWordsSet.contains(wordToDelete)) { // <-- Sử dụng tập hợp mới
            // Logic này trên lý thuyết không bao giờ được gọi nếu nút Delete chỉ bật cho user-added words,
            // nhưng là lớp bảo vệ tốt.
            showAlert("Thông báo", "Từ '" + wordToDelete + "' không phải từ người dùng thêm hoặc không có trong từ điển cục bộ để xóa.");
            setButtonState(false, true); // Add bật, Delete tắt
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

        // 1. Thử tra cứu định nghĩa cục bộ (từ cả file gốc và file thêm)
        String rawLocalDef = localDictionaryRaw != null ? localDictionaryRaw.get(cleanedWord) : null;

        if (rawLocalDef != null) {
            // Nếu tìm thấy cục bộ, format và hiển thị
            String formattedDef = definitionFormatter.formatLocalDefinition(cleanedWord, rawLocalDef);
            Platform.runLater(() -> {
                definitionArea.setText(formattedDef);
                // KIỂM TRA XEM TỪ CÓ PHẢI TỪ NGƯỜI DÙNG THÊM KHÔNG ĐỂ BẬT NÚT DELETE
                boolean isUserAdded = userAddedWordsSet != null && userAddedWordsSet.contains(cleanedWord); // <-- Sử dụng tập hợp mới
                setButtonState(false, !isUserAdded); // Add bật, Delete bật chỉ khi là user-added
            });
        } else {
            // Nếu không tìm thấy cục bộ, thông báo, và chuẩn bị gọi API
            Platform.runLater(() -> {
                definitionArea.setText("Không tìm thấy định nghĩa cục bộ cho '" + cleanedWord + "'. Đang tra cứu online...");
                setButtonState(false, true); // Bật nút Add (vì có thể thêm từ này), vô hiệu hóa nút Delete (vì không có cục bộ)
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
            userAddedWordsSet.add(word); // <-- THÊM từ vào tập hợp người dùng thêm
            masterWordList.add(word); // Thêm vào list hiển thị
            FXCollections.sort(masterWordList); // Sắp xếp lại list
        }

        // Cập nhật UI
        definitionArea.appendText("\n-- Đã thêm từ '" + word + "' vào từ điển cục bộ. --");
        System.out.println("Added word '" + word + "' to local dictionary.");

        // Khôi phục trạng thái nút: Add bật, Delete bật vì từ vừa thêm là user-added
        setButtonState(false, false); // <-- Cập nhật trạng thái nút (Add bật, Delete bật)
        // Tự động hiển thị định nghĩa của từ vừa thêm (nếu muốn)
        // displayWordDefinition(word);
    }

    // Helper để xử lý lỗi khi thêm từ trên luồng UI
    private void handleAddWordError(String word, String errorMessage) {
        System.err.println("Lỗi khi thêm từ '" + word + "': " + errorMessage);
        showAlert("Lỗi", "Không thể thêm từ '" + word + "' vào từ điển cục bộ: " + errorMessage);

        // Khôi phục trạng thái nút (Add bật, Delete dựa trên trạng thái CŨ)
        // Kiểm tra lại trạng thái delete cho từ hiện đang hiển thị sau khi lỗi
        String currentWordDisplayed = wordDisplayLabel.getText().replace("@", "").trim();
        boolean isCurrentWordUserAdded = userAddedWordsSet != null && userAddedWordsSet.contains(currentWordDisplayed);
        setButtonState(false, !isCurrentWordUserAdded); // Add bật, Delete tùy thuộc từ đang hiển thị
    }


    // Helper để xử lý kết quả xóa từ trên luồng UI
    private void handleDeleteWordResult(String word, boolean deletedFromFile) {
        if (deletedFromFile) {
            // Cập nhật bộ nhớ
            if (localDictionaryRaw != null) {
                localDictionaryRaw.remove(word);
            }
            userAddedWordsSet.remove(word); // <-- XÓA từ khỏi tập hợp người dùng thêm
            masterWordList.remove(word); // Xóa khỏi list hiển thị

            // Cập nhật UI
            wordDisplayLabel.setText("@" + word); // Vẫn hiển thị từ đã xóa trên label
            definitionArea.setText("Đã xóa từ '" + word + "' khỏi từ điển cục bộ.");
            System.out.println("Updated in-memory dictionary after deletion for '" + word + "'.");

            // Sau khi xóa thành công từ đang hiển thị, không có từ nào hiển thị nữa
            clearDefinition(); // Xóa nội dung hiển thị định nghĩa và vô hiệu hóa Delete
        } else {
            // Trường hợp không tìm thấy từ trong file user_added_words.txt để xóa
            // (nghĩa là từ đó không có trong tập userAddedWordsSet - điều này lý tưởng không xảy ra
            // nếu nút Delete chỉ được bật cho user-added words)
            definitionArea.appendText("\n-- Từ '" + word + "' không được tìm thấy trong file có thể sửa đổi (user added file) để xóa. --");
            System.err.println("Deletion attempt failed for word: " + word + " (Not found in user added file)");
            // Trạng thái nút: Add bật, Delete vẫn tắt (vì từ không còn trong tập userAddedWordsSet, hoặc chưa bao giờ có)
            setButtonState(false, true); // Add bật, Delete tắt
        }

        // Nút Add luôn được bật lại sau khi tác vụ nền hoàn thành (thành công hoặc thất bại)
        addButton.setDisable(false);
    }

    // Helper để xử lý lỗi khi xóa từ trên luồng UI
    private void handleDeleteWordError(String word, String errorMessage) {
        System.err.println("Lỗi khi xóa từ '" + word + "': " + errorMessage);
        showAlert("Lỗi", "Không thể xóa từ '" + word + "' khỏi từ điển cục bộ: " + errorMessage);

        // Khôi phục trạng thái nút (Add bật, Delete dựa trên trạng thái CŨ)
        // Kiểm tra lại trạng thái delete cho từ hiện đang hiển thị sau khi lỗi
        String currentWordDisplayed = wordDisplayLabel.getText().replace("@", "").trim();
        boolean isCurrentWordUserAdded = userAddedWordsSet != null && userAddedWordsSet.contains(currentWordDisplayed);
        setButtonState(false, !isCurrentWordUserAdded); // Add bật, Delete tùy thuộc từ đang hiển thị
    }


    // Helper để xử lý tra cứu API trên luồng nền
    private void fetchOnlineDefinition(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }
        String finalWord = word.trim();

        executorService.submit(() -> {
            String rawJsonResponse = null;
            try {
                rawJsonResponse = apiService.fetchDefinition(finalWord);

                String formattedApiDefinition;
                if (apiService.isNotFoundResponse(rawJsonResponse)) {
                    formattedApiDefinition = "Không tìm thấy định nghĩa online cho '" + finalWord + "'.";
                } else {
                    formattedApiDefinition = definitionFormatter.formatApiDefinition(rawJsonResponse);
                }

                Platform.runLater(() -> handleOnlineFetchSuccess(finalWord, formattedApiDefinition));

            } catch (IOException e) {
                Platform.runLater(() -> handleOnlineFetchError(finalWord, e.getMessage()));
            } catch (Exception e) {
                System.err.println("Lỗi xử lý phản hồi API cho '" + finalWord + "': " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> handleOnlineFetchError(finalWord, "Lỗi xử lý dữ liệu online: " + e.getMessage()));
            }
        });
    }

    // Helper để xử lý kết quả tra cứu online thành công (bao gồm cả không tìm thấy) trên luồng UI
    private void handleOnlineFetchSuccess(String word, String formattedDefinition) {
        // Kiểm tra xem có phải kết quả của từ hiện tại không
        String currentWordDisplayed = wordDisplayLabel.getText().replace("@", "").trim();
        if (!currentWordDisplayed.equalsIgnoreCase(word)) {
            System.out.println("Ignoring stale online result for '" + word + "' as current word displayed is different ('" + currentWordDisplayed + "').");
            return; // Bỏ qua kết quả cũ
        }

        boolean hasLocalDef = localDictionaryRaw != null && localDictionaryRaw.containsKey(word);

        if (hasLocalDef) {
            // Nếu đã có định nghĩa cục bộ, nối thêm định nghĩa online
            definitionArea.appendText("\n\n--- Định nghĩa online ---\n" + formattedDefinition);
        } else {
            // Nếu không có định nghĩa cục bộ, hiển thị định nghĩa online (ghi đè thông báo "Đang tra cứu...")
            definitionArea.setText(formattedDefinition);
        }
        // Trạng thái nút đã được set đúng ở displayWordDefinition (Add bật, Delete tắt khi không có local def)
    }

    // Helper để xử lý lỗi tra cứu online trên luồng UI
    private void handleOnlineFetchError(String word, String errorMessage) {
        System.err.println("Lỗi tra cứu online cho '" + word + "': " + errorMessage);

        // Kiểm tra xem có phải lỗi của từ hiện tại không
        String currentWordDisplayed = wordDisplayLabel.getText().replace("@", "").trim();
        if (!currentWordDisplayed.equalsIgnoreCase(word)) {
            System.out.println("Ignoring stale online error for '" + word + "' as current word displayed is different ('" + currentWordDisplayed + "').");
            return; // Bỏ qua lỗi cũ
        }

        String errorMsg = "Lỗi mạng hoặc kết nối khi tra cứu online '" + word + "': " + errorMessage;
        boolean hasLocalDef = localDictionaryRaw != null && localDictionaryRaw.containsKey(word);

        if (!hasLocalDef) {
            // Nếu không có định nghĩa cục bộ, hiển thị lỗi online (ghi đè thông báo "Đang tra cứu...")
            definitionArea.setText(errorMsg);
            // Trạng thái nút đã được set đúng ở displayWordDefinition (Add bật, Delete tắt)
            // setButtonState(false, true); // Add bật, Delete tắt
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
                try {
                    wordListView.scrollTo(filteredWordList.get(0));
                } catch (IndexOutOfBoundsException e) {
                    // Bỏ qua lỗi nếu filteredList rỗng ngay sau khi kiểm tra isEmpty()
                    System.err.println("Error scrolling ListView: " + e.getMessage());
                }
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
        addButton.setDisable(false); // Luôn bật nút thêm (có thể thêm bất kỳ từ nào nếu chưa tồn tại)
    }


    // Helper để hiển thị thông báo Alert
    private void showAlert(String title, String content) {
        // showAndWait() là blocking call, nên cần đảm bảo gọi từ luồng UI
        if (!Platform.isFxApplicationThread()) {
            System.err.println("showAlert called from non-UI thread!");
            // Có thể xử lý bằng cách chạy trên Platform.runLater nếu gọi từ luồng khác
            // hoặc chỉ cho phép gọi từ luồng UI như hiện tại trong các handler
            // Chạy trên UI thread nếu không phải
            Platform.runLater(() -> {
                Alert.AlertType alertType = title != null && title.toLowerCase().contains("lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
                Alert alert = new Alert(alertType);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            });
            return;
        }

        Alert.AlertType alertType = title != null && title.toLowerCase().contains("lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Helper để hiển thị hộp thoại xác nhận
    private Optional<ButtonType> showConfirmationDialog(String title, String content) {
        // showAndWait() là blocking call, nên cần đảm bảo gọi từ luồng UI
        if (!Platform.isFxApplicationThread()) {
            System.err.println("showConfirmationDialog called from non-UI thread!");
            // Có thể xử lý bằng cách chạy trên Platform.runLater nếu gọi từ luồng khác
            // hoặc chỉ cho phép gọi từ luồng UI như hiện tại trong handleQuitQuiz
            // Với dialog xác nhận, nên chạy trên UI thread và đợi kết quả
            Platform.runLater(() -> {
                // Không thể trả về Optional từ đây, cần cơ chế phức tạp hơn (ví dụ: CompletableFuture)
                // nếu muốn gọi từ non-UI thread và chờ kết quả đồng bộ.
                // Giả định chỉ gọi từ UI thread.
                showAlert("Lỗi nội bộ", "Hộp thoại xác nhận chỉ có thể gọi từ luồng UI.");
            });
            return Optional.empty(); // Trả về rỗng nếu không phải luồng UI
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait();
    }

    // Hàm để đóng ExecutorService khi ứng dụng tắt (cần gọi từ lớp Main Application)
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            System.out.println("Đóng ExecutorService cho DicController...");
            executorService.shutdownNow(); // Dùng shutdownNow để dừng ngay các tác vụ đang chạy
            System.out.println("ExecutorService cho DicController đã đóng.");
        }
    }
}