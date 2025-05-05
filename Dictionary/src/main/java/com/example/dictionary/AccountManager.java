package com.example.dictionary; // Khai báo package (gói) chứa lớp này. Giống như một thư mục để tổ chức code. Thay đổi "com.example.dictionary" cho phù hợp với dự án của bạn.

import java.util.HashMap; // Import lớp HashMap để sử dụng. HashMap là một cấu trúc dữ liệu lưu trữ cặp Key-Value hiệu quả.
import java.util.Map;     // Import giao diện Map. HashMap là một triển khai cụ thể của Map. Map định nghĩa cách hoạt động chung của các cấu trúc dữ liệu Key-Value.

/**
 * Lớp AccountManager quản lý việc đăng ký và đăng nhập tài khoản người dùng.
 * Sử dụng các phương thức static để có thể gọi trực tiếp qua tên lớp mà không cần tạo đối tượng.
 */
public class AccountManager {

    // Sử dụng static để biến này thuộc về chính lớp AccountManager, không phải một đối tượng cụ thể.
    // Chỉ có MỘT bản đồ 'accounts' duy nhất cho toàn bộ chương trình.
    // Sử dụng final để đảm bảo biến 'accounts' luôn trỏ đến cùng một đối tượng HashMap sau khi được khởi tạo.
    // Tuy nhiên, nội dung bên trong HashMap (các tài khoản) VẪN CÓ THỂ thay đổi (thêm/xóa).
    // Sử dụng private để ngăn truy cập trực tiếp vào danh sách tài khoản từ bên ngoài lớp này, đảm bảo tính đóng gói.
    // Map<String, String>: Khai báo một Map nơi Khóa (Key) là username (String) và Giá trị (Value) là password (String).
    private static final Map<String, String> accounts = new HashMap<>(); // Khởi tạo một HashMap trống để lưu trữ tài khoản.

    /**
     * Phương thức đăng ký một tài khoản người dùng mới.
     *
     * @param username Tên người dùng muốn đăng ký.
     * @param password Mật khẩu cho tài khoản mới.
     * @return true nếu đăng ký thành công, false nếu username đã tồn tại hoặc đầu vào không hợp lệ.
     */
    public static boolean signUp(String username, String password) {
        // --- Bước 1: Kiểm tra tính hợp lệ của đầu vào ---
        // Kiểm tra xem username hoặc password có bị null (không có giá trị) hoặc là chuỗi rỗng (sau khi loại bỏ khoảng trắng thừa) hay không.
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            System.err.println("Lỗi đăng ký: Username hoặc password không được để trống."); // In thông báo lỗi ra luồng lỗi chuẩn (stderr).
            return false; // Trả về false để báo hiệu đăng ký thất bại.
        }

        // --- Bước 2: Kiểm tra xem username đã tồn tại chưa ---
        // Sử dụng phương thức containsKey() của Map để kiểm tra xem 'username' đã có trong 'accounts' chưa.
        if (accounts.containsKey(username)) {
            System.err.println("Lỗi đăng ký: Username '" + username + "' đã tồn tại."); // Thông báo lỗi nếu username đã có.
            return false; // Trả về false vì không thể đăng ký username trùng lặp.
        }

        // --- Bước 3: Thêm tài khoản mới vào Map ---
        // Nếu mọi kiểm tra đều ổn, dùng phương thức put() để thêm cặp username (Key) và password (Value) vào HashMap 'accounts'.
        accounts.put(username, password); // Lưu trữ tài khoản mới.
        System.out.println("Đăng ký thành công cho user: " + username); // In thông báo thành công ra luồng chuẩn (stdout).
        return true; // Trả về true để báo hiệu đăng ký thành công.
    }

    /**
     * Phương thức xác thực thông tin đăng nhập của người dùng.
     *
     * @param username Tên người dùng nhập vào để đăng nhập.
     * @param password Mật khẩu nhập vào để đăng nhập.
     * @return true nếu username tồn tại và mật khẩu khớp, false trong trường hợp ngược lại.
     */
    public static boolean validateLogin(String username, String password) {
        // --- Bước 1: Kiểm tra xem username có tồn tại không ---
        // Dấu '!' là toán tử NOT (phủ định). Kiểm tra xem 'accounts' KHÔNG chứa khóa 'username'.
        if (!accounts.containsKey(username)) {
            System.err.println("Lỗi đăng nhập: Username '" + username + "' không tồn tại."); // Thông báo lỗi nếu username không tìm thấy.
            return false; // Trả về false vì không thể đăng nhập với user không tồn tại.
        }

        // --- Bước 2: Lấy mật khẩu đã lưu trữ của username ---
        // Nếu username tồn tại, dùng phương thức get() để lấy mật khẩu (Value) tương ứng với username (Key) đó từ Map.
        String storedPassword = accounts.get(username);

        // --- Bước 3: So sánh mật khẩu đã lưu với mật khẩu nhập vào ---
        // **QUAN TRỌNG:** Sử dụng phương thức .equals() để so sánh nội dung của hai chuỗi String.
        // KHÔNG dùng toán tử '==' để so sánh chuỗi vì nó so sánh địa chỉ bộ nhớ, thường sẽ không đúng ý muốn.
        boolean isValid = storedPassword.equals(password); // 'isValid' sẽ là true nếu mật khẩu khớp, ngược lại là false.

        // --- Bước 4: Thông báo nếu sai mật khẩu ---
        if (!isValid) { // Nếu 'isValid' là false (mật khẩu không khớp)
            System.err.println("Lỗi đăng nhập: Sai mật khẩu cho user: " + username); // Thông báo lỗi sai mật khẩu.
        }

        // --- Bước 5: Trả về kết quả xác thực ---
        return isValid; // Trả về true nếu mật khẩu đúng, false nếu sai.
    }

    // --- Khối khởi tạo tĩnh (Static Initializer Block) ---
    // Đoạn code trong khối này sẽ được thực thi TỰ ĐỘNG và CHỈ MỘT LẦN DUY NHẤT
    // ngay khi lớp AccountManager được nạp vào bộ nhớ lần đầu tiên.
    static {
        // Tự động gọi phương thức signUp để thêm một tài khoản mẫu vào danh sách.
        // Mục đích: Có sẵn dữ liệu để kiểm thử (test) ngay khi chương trình chạy mà không cần đăng ký thủ công.
        signUp("testuser", "password123");
        System.out.println("Tài khoản testuser đã được tự động tạo."); // Thông báo rằng tài khoản test đã được thêm.
    }
}