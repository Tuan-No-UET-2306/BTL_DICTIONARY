package com.example.dictionary; // Thay đổi package cho phù hợp

import java.util.HashMap;
import java.util.Map;

public class AccountManager {

    // Sử dụng static để dễ truy cập trong ví dụ này
    // Key: username, Value: password
    private static final Map<String, String> accounts = new HashMap<>();

    // Phương thức đăng ký tài khoản mới
    public static boolean signUp(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            System.err.println("Username hoặc password không được để trống.");
            return false; // Không hợp lệ
        }
        if (accounts.containsKey(username)) {
            System.err.println("Username '" + username + "' đã tồn tại.");
            return false; // Tài khoản đã tồn tại
        }
        accounts.put(username, password);
        System.out.println("Đăng ký thành công cho user: " + username);
        return true; // Đăng ký thành công
    }

    // Phương thức xác thực đăng nhập
    public static boolean validateLogin(String username, String password) {
        if (!accounts.containsKey(username)) {
            System.err.println("Username '" + username + "' không tồn tại.");
            return false; // User không tồn tại
        }
        String storedPassword = accounts.get(username);
        boolean isValid = storedPassword.equals(password);
        if (!isValid) {
            System.err.println("Sai mật khẩu cho user: " + username);
        }
        return isValid; // Trả về true nếu mật khẩu khớp
    }

    // (Tùy chọn) Thêm một tài khoản mẫu để test
    static {
        signUp("testuser", "password123");
    }
}