package com.example.dictionary; // Khai báo package chứa lớp này. Đảm bảo khớp với cấu trúc dự án của bạn.

// Import các lớp cần thiết từ thư viện JavaFX để làm việc với giao diện người dùng
import javafx.fxml.FXML;             // Dùng để liên kết các thành phần trong file FXML với code Java.
import javafx.fxml.FXMLLoader;     // Dùng để tải file FXML (định nghĩa giao diện).
import javafx.scene.Parent;         // Lớp cha cho tất cả các node (thành phần) trong cây giao diện.
import javafx.scene.Scene;           // Đại diện cho toàn bộ nội dung trong một cửa sổ (Stage).
import javafx.scene.control.Button;  // Lớp cho thành phần nút bấm.
import javafx.scene.control.Label;    // Lớp cho thành phần nhãn hiển thị văn bản.
import javafx.scene.control.PasswordField; // Lớp cho thành phần nhập mật khẩu (ẩn ký tự).
import javafx.scene.control.TextField; // Lớp cho thành phần nhập văn bản thông thường.
import javafx.scene.image.Image;     // Lớp để tải và quản lý hình ảnh.
import javafx.scene.image.ImageView; // Lớp để hiển thị hình ảnh trên giao diện.
import javafx.scene.paint.Color;    // Lớp định nghĩa các màu sắc (ví dụ: RED, GREEN).
import javafx.stage.Stage;         // Đại diện cho cửa sổ ứng dụng (window).

// Import các lớp Java cơ bản khác
import java.io.IOException;         // Dùng để xử lý các lỗi liên quan đến Input/Output (ví dụ: đọc file).
import java.io.InputStream;       // Dùng để đọc dữ liệu từ một nguồn (ví dụ: file resource).

/**
 * Lớp Controller điều khiển các sự kiện và logic cho màn hình đăng nhập (login.fxml).
 * Nó kết nối giao diện người dùng (được định nghĩa trong FXML) với logic xử lý phía sau (Java).
 */
public class LoginController {

    // --- Liên kết các thành phần UI từ FXML ---
    // Annotation @FXML đánh dấu các biến này sẽ được tự động gán giá trị
    // tương ứng với các thành phần có cùng `fx:id` trong file login.fxml.

    @FXML private TextField usernameField;      // Ô nhập liệu cho tên người dùng (fx:id="usernameField").
    @FXML private PasswordField passwordField;  // Ô nhập liệu cho mật khẩu (fx:id="passwordField").
    @FXML private Button loginButton;          // Nút bấm để thực hiện đăng nhập (fx:id="loginButton").
    @FXML private Button signUpButton;         // Nút bấm để thực hiện đăng ký (fx:id="signUpButton").
    @FXML private Label messageLabel;         // Nhãn để hiển thị thông báo lỗi hoặc thành công (fx:id="messageLabel").
    @FXML private ImageView logoImageView;      // Vùng hiển thị hình ảnh logo (fx:id="logoImageView").

    /**
     * Phương thức `initialize` được JavaFX tự động gọi *sau khi* file FXML đã được tải
     * và tất cả các thành phần @FXML đã được liên kết.
     * Dùng để thực hiện các cài đặt ban đầu cho giao diện.
     */
    @FXML
    public void initialize() {
        // --- Tải và hiển thị logo (Tùy chọn) ---
        try {
            // Cố gắng lấy luồng dữ liệu (InputStream) của file ảnh "logo.png".
            // getClass().getResourceAsStream() tìm file trong cùng thư mục resource với file .class này.
            // **QUAN TRỌNG:** Đảm bảo file "logo.png" nằm trong thư mục resources của dự án và được cấu hình đúng trong môi trường build (Maven/Gradle).
            InputStream logoStream = getClass().getResourceAsStream("mainBackGround.png"); // Thay "logo.png" bằng tên file logo thực tế của bạn.

            if (logoStream != null) { // Nếu tìm thấy file logo
                logoImageView.setImage(new Image(logoStream)); // Tạo đối tượng Image từ luồng dữ liệu và đặt nó cho ImageView.
            } else { // Nếu không tìm thấy file logo trong resources
                System.err.println("Lỗi: Không tìm thấy file logo 'logo.png' trong resources.");
                // Ẩn ImageView đi nếu không có logo
                logoImageView.setVisible(false); // Làm cho nó không hiển thị.
                logoImageView.setManaged(false); // Không dành không gian layout cho nó nữa.
            }
        } catch (Exception e) { // Bắt các lỗi có thể xảy ra trong quá trình tải ảnh
            System.err.println("Lỗi khi tải logo: " + e.getMessage());
            // Cũng ẩn ImageView nếu có lỗi xảy ra
            logoImageView.setVisible(false);
            logoImageView.setManaged(false);
        }

        // --- Xóa nội dung ban đầu của nhãn thông báo ---
        messageLabel.setText(""); // Đảm bảo nhãn thông báo trống khi màn hình mới hiển thị.
    }


    /**
     * Phương thức xử lý sự kiện khi người dùng nhấn nút "Login".
     * Được liên kết với thuộc tính `onAction` của nút Login trong file FXML.
     */
    @FXML
    private void handleLogin() {
        // Lấy giá trị từ ô username và loại bỏ khoảng trắng thừa ở đầu/cuối.
        String username = usernameField.getText().trim();
        // Lấy giá trị từ ô password. Thường không nên trim() mật khẩu.
        String password = passwordField.getText();

        // Kiểm tra xem người dùng đã nhập đủ thông tin chưa.
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Tên đăng nhập và mật khẩu không được để trống.", true); // Hiển thị thông báo lỗi (isError = true).
            return; // Dừng xử lý nếu thiếu thông tin.
        }

        // Gọi phương thức validateLogin từ lớp AccountManager để kiểm tra thông tin đăng nhập.
        if (AccountManager.validateLogin(username, password)) {
            // Nếu đăng nhập thành công
            showMessage("Đăng nhập thành công!", false); // Hiển thị thông báo thành công (isError = false).
            switchToMainScene(); // Chuyển sang màn hình chính của ứng dụng.
        } else {
            // Nếu đăng nhập thất bại (sai username hoặc password)
            // AccountManager đã in lỗi cụ thể hơn ra console, ở đây chỉ cần thông báo chung.
            showMessage("Tên đăng nhập hoặc mật khẩu không hợp lệ.", true); // Hiển thị thông báo lỗi.
        }
    }

    /**
     * Phương thức xử lý sự kiện khi người dùng nhấn nút "Sign Up".
     * Được liên kết với thuộc tính `onAction` của nút Sign Up trong file FXML.
     */
    @FXML
    private void handleSignUp() {
        // Lấy thông tin username và password từ các ô nhập liệu.
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Kiểm tra xem người dùng đã nhập đủ thông tin để đăng ký chưa.
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Cần nhập Tên đăng nhập và Mật khẩu để đăng ký.", true); // Thông báo lỗi.
            return; // Dừng xử lý.
        }

        // Gọi phương thức signUp từ lớp AccountManager để thử đăng ký tài khoản mới.
        if (AccountManager.signUp(username, password)) {
            // Nếu đăng ký thành công
            showMessage("Đăng ký thành công! Bây giờ bạn có thể đăng nhập.", false); // Thông báo thành công.
            passwordField.clear(); // Xóa trắng ô mật khẩu sau khi đăng ký thành công.
            usernameField.clear(); // (Tùy chọn) Xóa trắng cả ô username.
            usernameField.requestFocus(); // Đưa con trỏ nhập liệu về lại ô username để tiện đăng nhập.
        } else {
            // Nếu đăng ký thất bại (thường là do username đã tồn tại hoặc không hợp lệ theo logic của AccountManager).
            // AccountManager đã in lỗi cụ thể ra console.
            showMessage("Tên đăng nhập đã tồn tại hoặc không hợp lệ.", true); // Thông báo lỗi chung.
        }
    }

    /**
     * Phương thức tiện ích để hiển thị thông báo trên messageLabel.
     * Có thể thay đổi màu sắc hoặc style của thông báo tùy thuộc vào đó là lỗi hay thành công.
     *
     * @param message Nội dung thông báo cần hiển thị.
     * @param isError `true` nếu là thông báo lỗi (thường màu đỏ), `false` nếu là thông báo thành công (thường màu xanh).
     */
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message); // Đặt nội dung văn bản cho Label.

        // --- Áp dụng CSS Styling (Tùy chọn nhưng nên dùng) ---
        // Xóa các style class cũ trước khi thêm style mới để tránh xung đột.
        messageLabel.getStyleClass().removeAll("success-message", "error-message");

        if (isError) {
            // Thêm style class "error-message". Bạn cần định nghĩa class này trong file CSS
            // Ví dụ trong file CSS: .error-message { -fx-text-fill: red; -fx-font-weight: bold; }
            messageLabel.getStyleClass().add("error-message");
            // Cài đặt màu chữ trực tiếp làm phương án dự phòng nếu CSS không hoạt động hoặc không được định nghĩa.
            messageLabel.setTextFill(Color.RED);
        } else {
            // Thêm style class "success-message".
            // Ví dụ trong file CSS: .success-message { -fx-text-fill: green; }
            messageLabel.getStyleClass().add("success-message");
            // Phương án dự phòng
            messageLabel.setTextFill(Color.GREEN);
        }
    }

    /**
     * Phương thức để chuyển từ màn hình đăng nhập sang màn hình chính (main.fxml).
     * Được gọi sau khi đăng nhập thành công.
     */
    private void switchToMainScene() {
        try {
            // 1. Lấy Stage (cửa sổ) hiện tại từ một thành phần bất kỳ trên Scene hiện tại (ví dụ: loginButton).
            Stage currentStage = (Stage) loginButton.getScene().getWindow();

            // 2. Tạo một FXMLLoader để tải file FXML của màn hình chính.
            // Đảm bảo "main.fxml" nằm trong cùng thư mục resources hoặc có đường dẫn đúng.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));

            // 3. Tải FXML và lấy node gốc (thường là Pane, AnchorPane, VBox, ...) của màn hình chính.
            Parent root = loader.load(); // Quá trình này có thể ném ra IOException.

            // 4. Tạo một Scene mới chứa nội dung của màn hình chính.
            Scene mainScene = new Scene(root);

            // 5. (Tùy chọn) Thêm file CSS cho màn hình chính nếu cần.
            // mainScene.getStylesheets().add(getClass().getResource("main-styles.css").toExternalForm());

            // 6. Đặt Scene mới này làm Scene hiện tại của cửa sổ (Stage).
            currentStage.setScene(mainScene);

            // 7. (Tùy chọn) Cập nhật tiêu đề cửa sổ.
            currentStage.setTitle("Từ điển Anh-Việt"); // Thay đổi tiêu đề cho phù hợp.

            // 8. (Tùy chọn) Đặt lại kích thước cửa sổ hoặc căn giữa màn hình.
            currentStage.centerOnScreen(); // Căn cửa sổ ra giữa màn hình.
            currentStage.setResizable(true); // Cho phép người dùng thay đổi kích thước cửa sổ chính (nếu muốn).
            // currentStage.sizeToScene(); // Điều chỉnh kích thước cửa sổ vừa với nội dung scene mới

        } catch (IOException e) {
            // Xử lý lỗi nếu không thể tải được file main.fxml.
            e.printStackTrace(); // In chi tiết lỗi ra console để debug.
            showMessage("Lỗi nghiêm trọng: Không thể tải màn hình chính.", true); // Hiển thị lỗi trên màn hình đăng nhập.
        }
    }
}