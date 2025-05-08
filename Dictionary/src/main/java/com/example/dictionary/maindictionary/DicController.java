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
import javafx.scene.control.Alert.AlertType;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;
import java.util.HashSet;


public class DicController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label wordDisplayLabel;
    @FXML private ListView<String> wordListView;
    @FXML private TextArea definitionArea;
    @FXML private Button backButton;
    @FXML private Button addButton;
    @FXML private Button deleteButton;

    private final ObservableList<String> listTuChinh = FXCollections.observableArrayList();
    private FilteredList<String> filteredWordList;
    private Map<String, String> tuDienTho;
    private Set<String> userAddedWordsSet = new HashSet<>();

    private DicDataLoader dataLoader;
    private DicApiService apiService;
    private DefinitionFormatter dinhDangChuoi;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupServices();
        loadInitialData();
        setupListView();
        setupSearchFieldListener();
        setupButtonActions();
        setupInitialUIState();
    }

    private void setupServices() {
        dataLoader = new DicDataLoader("/com/example/dictionary/dic_words.txt");
        apiService = new DicApiService();
        dinhDangChuoi = new DefinitionFormatter();
    }
    private void setButtonState(boolean disableAdd, boolean disableDelete) {
        addButton.setDisable(disableAdd);
        deleteButton.setDisable(disableDelete);
    }

    private void loadInitialData() { // tải dữ liệu để hiện thị và sử dụng
        Platform.runLater(() -> {
            wordDisplayLabel.setText("");
            definitionArea.setText("Đang tải dữ liệu từ điển...");
            definitionArea.setPromptText("");
            setButtonState(true, true);
        });

        executorService.submit(() -> {
            try {
                DicLoadResult loadResult = dataLoader.loadWordsAndRawDefinitions();
                tuDienTho = loadResult.allWords;
                userAddedWordsSet = loadResult.userAddedWords;

                Platform.runLater(() -> {
                    listTuChinh.clear();
                    if (tuDienTho != null && !tuDienTho.isEmpty()) {
                        listTuChinh.addAll(tuDienTho.keySet());// trả về các keys trong map
                    }
                    FXCollections.sort(listTuChinh);
                    if (!listTuChinh.isEmpty()) {
                        System.out.println("5 từ đầu tiên trong danh sách chính:");
                        for (int i = 0; i < Math.min(5, listTuChinh.size()); i++) {
                            System.out.println("  - " + listTuChinh.get(i));
                        }
                    } else {
                        System.out.println("Danh sách chính rỗng.");
                    }
                    System.out.println("----------------------------------");


                    if (tuDienTho != null && !tuDienTho.isEmpty()) {
                        definitionArea.setText("Dữ liệu từ điển đã tải xong. Sẵn sàng tra cứu.");
                        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
                    } else {
                        definitionArea.setText("Không tải được dữ liệu từ điển hoặc từ điển trống.");
                        definitionArea.setPromptText("Vui lòng kiểm tra file dữ liệu.");
                    }
                    setButtonState(false, true);
                });

            } catch (RuntimeException e) {
                System.err.println("Lỗi khi tải dữ liệu từ điển: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Lỗi tải dữ liệu", "Không thể tải dữ liệu từ điển ban đầu: " + e.getMessage());
                    tuDienTho = new HashMap<>();
                    userAddedWordsSet = new HashSet<>();
                    listTuChinh.clear();
                    definitionArea.setText("Lỗi tải dữ liệu từ điển.");
                    setButtonState(false, true);
                });
            }
        });
    }
//thiết lập và cấu hình thành phần  ListView: phanaf gợi ý
    private void setupListView() {
        //hiện thị phiên bản đã đươc lọc mà không  thay đổi ds gốc
        filteredWordList = new FilteredList<>(listTuChinh, p -> true);
        wordListView.setItems(filteredWordList);
        wordListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                displayWordDefinition(newValue);
            }
        });
    }

    private void setupSearchFieldListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("Văn bản tìm kiếm được thay đổi thành: '" + newValue + "'. Đang gọi filterWordList...");
            filterWordList(newValue);
        });
        searchField.setOnAction(event -> handleSearchAction());
    }

    private void setupButtonActions() {
        searchButton.setOnAction(event -> handleSearchAction());
        addButton.setOnAction(event -> handleAddWordAction());
        deleteButton.setOnAction(event -> handleDeleteWordAction());
        backButton.setOnAction(event -> handleBack(event));
    }

    private void setupInitialUIState() {
        definitionArea.setPromptText("Đang tải dữ liệu...");
        definitionArea.setEditable(false);// ngăn k cho ng dùng sửa TextArea
        setButtonState(true, true);
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/dictionary/main.fxml")); // Đường dẫn đến main FXML
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Main Application");
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi khi tải main.fxml: " + e.getMessage());
            showAlert("Lỗi", "Không thể quay lại màn hình chính.");
        }
    }

    @FXML
    private void handleSearchAction() {//xử lý khi ng dùng search
        String tuTimKiem = searchField.getText().trim();
        if (!tuTimKiem.isEmpty()) {
            if (filteredWordList.contains(tuTimKiem)) {
                displayWordDefinition(tuTimKiem);
            }
       }
          else {
            wordListView.getSelectionModel().clearSelection();
            xoaDinhNghia();
        }
    }

    @FXML
    // xử lý khi thêm từ.
    //ktra từ tồn tại ch ->thông báo thêm -> thêm vào file luồng nền
    private void handleAddWordAction() {
        String wordToAdd = searchField.getText().trim();
        if (wordToAdd.isEmpty()) {
            showAlert("Lỗi", "Không có từ nào để thêm.");
            return;
        }
        if (tuDienTho != null && tuDienTho.containsKey(wordToAdd)) {
            showAlert("Thông báo", "Từ '" + wordToAdd + "' đã có trong từ điển.");
            displayWordDefinition(wordToAdd);// hiện thị lại định nghĩa
            return;
        }

        setButtonState(true, true);
        definitionArea.appendText("\n-- Đang thêm '" + wordToAdd + "'... --");

        final String finalWordToAdd = wordToAdd;
        executorService.submit(() -> {
            try {
                String defaultRawDefinition = "Chưa có định nghĩa nào được cung cấp.";
                String lineToAppend = finalWordToAdd + " " + defaultRawDefinition;
                UserWordFileManager.appendLineToFile(lineToAppend);// gọi class ... để ghi vào file

                final String finalDefaultRawDefinition = defaultRawDefinition;
                Platform.runLater(() -> handleAddWordSuccess(finalWordToAdd, finalDefaultRawDefinition));
            } catch (IOException e) {// xử lý lõi ghi file
                final String errorMsg = e.getMessage();
                Platform.runLater(() -> handleAddWordError(finalWordToAdd, errorMsg));
            }
        });
    }

    @FXML
    private void handleDeleteWordAction() {
        String wordToDelete = wordDisplayLabel.getText().replace("@", "").trim();
        if (wordToDelete.isEmpty()) {
            showAlert("Lỗi", "Không có từ nào đang hiển thị để xóa.");
            return;
        }

        // Chỉ cho phép xóa nếu từ đó là từ người dùng thêm
        if (userAddedWordsSet == null || !userAddedWordsSet.contains(wordToDelete)) {
            showAlert("Thông báo", "Từ '" + wordToDelete + "' không phải từ người dùng thêm nên không thể xóa.");
            // Trạng thái nút delete đã được set đúng ở displayWordDefinition
            return;
        }

        Optional<ButtonType> result = showConfirmationDialog("Xác nhận xóa", "Bạn có chắc muốn xóa từ '" + wordToDelete + "' khỏi từ điển người dùng thêm không?");
        if (!result.isPresent() || result.get() != ButtonType.OK) {
            return; // Người dùng hủy
        }

        setButtonState(true, true); // Vô hiệu hóa nút trong khi xóa
        definitionArea.appendText("\n-- Đang xóa '" + wordToDelete + "'... --");

        final String finalWordToDelete = wordToDelete; // Biến final cho lambda
        executorService.submit(() -> {
            try {
                boolean deletedFromFile = com.example.dictionary.maindictionary.UserWordFileManager.deleteWordFromFile(finalWordToDelete);
                Platform.runLater(() -> handleDeleteWordResult(finalWordToDelete, deletedFromFile));
            } catch (IOException e) {
                final String errorMsg = e.getMessage(); // Biến final cho lambda
                Platform.runLater(() -> handleDeleteWordError(finalWordToDelete, errorMsg));
            }
        });
    }
    private void displayWordDefinition(String word) {
        if (word == null || word.trim().isEmpty()) {
            xoaDinhNghia();
            return;
        }
        String cleanedWord = word.trim();
        wordDisplayLabel.setText("@" + cleanedWord);
        definitionArea.setText("Đang xử lý '" + cleanedWord + "'...");

        String rawLocalDef = tuDienTho != null ? tuDienTho.get(cleanedWord) : null;

        if (rawLocalDef != null) {
            String formattedDef = dinhDangChuoi.formatLocalDefinition(cleanedWord, rawLocalDef);
            Platform.runLater(() -> {
                definitionArea.setText(formattedDef);
                boolean isUserAdded = userAddedWordsSet != null && userAddedWordsSet.contains(cleanedWord);
                setButtonState(false, !isUserAdded); // Add bật, Delete bật nếu là user-added
            });
        } else {
            Platform.runLater(() -> {
                definitionArea.setText("Không tìm thấy định nghĩa cục bộ cho '" + cleanedWord + "'. Đang tra cứu online...");
                setButtonState(false, true);
            });
            fetchOnlineDefinition(cleanedWord);
        }
    }

    private void handleAddWordSuccess(String word, String rawDefinition) {
        if (tuDienTho != null) {
            tuDienTho.put(word, rawDefinition);
            userAddedWordsSet.add(word);
            listTuChinh.add(word);
            FXCollections.sort(listTuChinh);
        }
        definitionArea.appendText("\n-- Đã thêm từ '" + word + "' vào từ điển. --");
        System.out.println("Đã thêm từ '" + word + "'. Kích thước dánh sách: " + listTuChinh.size());
        setButtonState(false, false);
    }

    private void handleAddWordError(String word, String errorMessage) {
        showAlert("Lỗi", "Không thể thêm từ '" + word + "': " + errorMessage);
        String currentWordDisplayed = wordDisplayLabel.getText().replace("@", "").trim();
        boolean isCurrentWordUserAdded = userAddedWordsSet != null && userAddedWordsSet.contains(currentWordDisplayed);
        setButtonState(false, !isCurrentWordUserAdded);
    }

    private void handleDeleteWordResult(String word, boolean deletedFromFile) {
        if (deletedFromFile) {
            if (tuDienTho != null) tuDienTho.remove(word);
            userAddedWordsSet.remove(word);
            listTuChinh.remove(word);


            definitionArea.setText("Đã xóa từ '" + word + "' khỏi từ điển người dùng thêm.");
            System.out.println("Deleted word '" + word + "'. MasterWordList size: " + listTuChinh.size());
            xoaDinhNghia();
        } else {
            definitionArea.appendText("\n-- Không thể xóa từ '" + word + "' khỏi file. Từ có thể không tồn tại trong file người dùng thêm. --");
            setButtonState(false, true);
        }
        addButton.setDisable(false);
    }

    private void handleDeleteWordError(String word, String errorMessage) {
        showAlert("Lỗi", "Không thể xóa từ '" + word + "': " + errorMessage);
        String currentWordDisplayed = wordDisplayLabel.getText().replace("@", "").trim();
        boolean isCurrentWordUserAdded = userAddedWordsSet != null && userAddedWordsSet.contains(currentWordDisplayed);
        // nếu là từ do ng dùng thêm thì bật lại nút delete
        setButtonState(false, !isCurrentWordUserAdded);
    }
// khởi tào và định nghĩa từ
    private void fetchOnlineDefinition(String word) {
        if (word == null || word.trim().isEmpty()) return;
        final String finalWord = word.trim();
        executorService.submit(() -> {// gửi tác vụ cho api
            try {
                String rawJsonResponse = apiService.guiYeuCau(finalWord);
                String formattedApiDefinition;
                if (apiService.isNotFoundResponse(rawJsonResponse)) {
                    formattedApiDefinition = "Không tìm thấy định nghĩa online cho '" + finalWord + "'.";
                } else {
                    formattedApiDefinition = dinhDangChuoi.formatChuoi(rawJsonResponse);
                }
                Platform.runLater(() -> handleOnlineFetchSuccess(finalWord, formattedApiDefinition));
            } catch (IOException e) {// lõi mạng
                final String errorMsg = e.getMessage();
                Platform.runLater(() -> handleOnlineFetchError(finalWord, errorMsg));
            }
        });
    }
// cập nhật giao diện, hiển thị đinh nghia
    private void handleOnlineFetchSuccess(String word, String formattedDefinition) {
        String currentWordDisplayed = wordDisplayLabel.getText().replace("@", "").trim();
        if (!currentWordDisplayed.equalsIgnoreCase(word)) {
            return;
        }
        // nối or chèn
        //ktra từ này có ở trong file cục bộ k
        boolean hasLocalDef = tuDienTho != null && tuDienTho.containsKey(word);
        if (hasLocalDef) {
            definitionArea.appendText("\n\n--- Định nghĩa online ---\n" + formattedDefinition);
        } else {
            definitionArea.setText(formattedDefinition);
        }

    }
// cập nhật giao diện ng dùng xảy ra lỗi
    private void handleOnlineFetchError(String word, String errorMessage) {
        //lấy từ vựng hiện đang được hiển thị trên giao diện
        String currentWordDisplayed = wordDisplayLabel.getText().replace("@", "").trim();
        if (!currentWordDisplayed.equalsIgnoreCase(word)) {
            return;
        }
        String errorMsg = "Lỗi mạng hoặc kết nối khi tra cứu online '" + word + "': " + errorMessage;
        // ktra từ word có tồn tại trong từ điển cục bộ k
        boolean hasLocalDef = tuDienTho != null && tuDienTho.containsKey(word);
        if (!hasLocalDef) {
            definitionArea.setText(errorMsg);
        } else {
            definitionArea.appendText("\n\n--- Lỗi khi tra cứu online ---\n" + errorMsg);
        }
    }
// Lọc danh sách trong ListView dựa trên nd ng dùng gõ vào thanh tìm kiếm
    private void filterWordList(String filter) {
        System.out.println("Đang lọc với: '" + filter + "'. Kích thước từ chính: " + listTuChinh.size());
        if (filter == null || filter.isEmpty()) {
            filteredWordList.setPredicate(p -> true);// luôn tar về true cho bất cứ phần từ nào
        } else {
            String lowerCaseFilter = filter.toLowerCase();// chuyến sang chữ thường
            filteredWordList.setPredicate(word -> word.toLowerCase().startsWith(lowerCaseFilter));
        }

        System.out.println("Kích thước danh sách lọc: " + filteredWordList.size());
        if (!filteredWordList.isEmpty()) {
            System.out.println("\n" +
                    "Mục đầu tiên trong Danh sách đã lọc: " + filteredWordList.get(0));
            Platform.runLater(() -> {
                try {// try- catch giúp TH ds thay đổi nhanh trong lúc
                    wordListView.scrollTo(0); // ktra rỗng và lúc gọi scollTo
                } catch (Exception e) {
                    System.err.println("Lỗi khi cuộn/lựa chọn trong ListView: " + e.getMessage());
                }
            });
        } else {
            System.out.println("Danh sách đã lọc đang trống.");
        }
    }
// đặt lại giao diện về hiển thị định nghĩa luc đầu or lúc rỗng
    private void xoaDinhNghia() {
        wordDisplayLabel.setText("");
        definitionArea.clear();
        definitionArea.setPromptText("Chọn một từ từ danh sách hoặc nhập từ vào ô 'Tra từ' và nhấn nút.");
        setButtonState(false, true);
    }

    private void showAlert(String title, String content) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showAlert(title, content));
            return;
        }
        AlertType alertType = title != null && title.toLowerCase().contains("lỗi") ? AlertType.ERROR : AlertType.INFORMATION;
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
//Nhiệm vụ chính của nó là hiển thị một hộp thoại xác nhận
// (Confirmation Alert dialog) cho người dùng và trả về kết quả
// cho biết người dùng đã chọn nút nào
    private Optional<ButtonType> showConfirmationDialog(String title, String content) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showAlert("Lỗi nội bộ", "Hộp thoại xác nhận chỉ có thể gọi từ luồng UI."));
            return Optional.empty();
        }
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait();
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            System.out.println("ExecutorService cho DicController đã đóng.");
        }
    }
}