package com.example.dictionary; // Khai báo package chứa lớp này. Đảm bảo khớp với cấu trúc dự án của bạn.

// Import các lớp cần thiết từ thư viện JavaFX để làm việc với giao diện người dùng
import Function.ChangeStage;
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
            showMessage("Tên đăng nhập và mật khẩu không được để trống."); // Hiển thị thông báo lỗi (isError = true).
            return; // Dừng xử lý nếu thiếu thông tin.
        }

        // Gọi phương thức validateLogin từ lớp AccountManager để kiểm tra thông tin đăng nhập.
        if (AccountManager.validateLogin(username, password)) {
            // Nếu đăng nhập thành công
            showMessage("Đăng nhập thành công!"); // Hiển thị thông báo thành công (isError = false).
            switchToMainScene(); // Chuyển sang màn hình chính của ứng dụng.
        } else {
            // Nếu đăng nhập thất bại (sai username hoặc password)
            // AccountManager đã in lỗi cụ thể hơn ra console, ở đây chỉ cần thông báo chung.
            showMessage("Tên đăng nhập hoặc mật khẩu không hợp lệ."); // Hiển thị thông báo lỗi.
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
            showMessage("Cần nhập Tên đăng nhập và Mật khẩu để đăng ký."); // Thông báo lỗi.
            return; // Dừng xử lý.
        }

        // Gọi phương thức signUp từ lớp AccountManager để thử đăng ký tài khoản mới.
        if (AccountManager.signUp(username, password)) {
            // Nếu đăng ký thành công
            showMessage("Đăng ký thành công! Bây giờ bạn có thể đăng nhập."); // Thông báo thành công.
            passwordField.clear(); // Xóa trắng ô mật khẩu sau khi đăng ký thành công.
            usernameField.clear(); // (Tùy chọn) Xóa trắng cả ô username.
            usernameField.requestFocus(); // Đưa con trỏ nhập liệu về lại ô username để tiện đăng nhập.
        } else {
            // Nếu đăng ký thất bại (thường là do username đã tồn tại hoặc không hợp lệ theo logic của AccountManager).
            // AccountManager đã in lỗi cụ thể ra console.
            showMessage("Tên đăng nhập đã tồn tại hoặc không hợp lệ."); // Thông báo lỗi chung.
        }
    }

    /**
     * Phương thức tiện ích để hiển thị thông báo trên messageLabel.
     * Có thể thay đổi màu sắc hoặc style của thông báo tùy thuộc vào đó là lỗi hay thành công.
     *
     * @param message Nội dung thông báo cần hiển thị.
     */
    private void showMessage(String message) {
        messageLabel.setText(message); // Đặt nội dung văn bản cho Label.

        // --- Áp dụng CSS Styling (Tùy chọn nhưng nên dùng) ---
        // Xóa các style class cũ trước khi thêm style mới để tránh xung đột.



    }

    /**
     * Phương thức để chuyển từ màn hình đăng nhập sang màn hình chính (main.fxml).
     * Được gọi sau khi đăng nhập thành công.
     */
    private void switchToMainScene() {
        ChangeStage.changeStage(loginButton, "main.fxml", getClass());
    }
}