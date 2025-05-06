package com.example.dictionary.maindictionary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream; // <-- Import cần thiết
import java.io.FileOutputStream; // <-- Import cần thiết
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter; // <-- Import cần thiết
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UserWordFileManager {

    // --- Đường dẫn cố định đến file lưu trữ từ người dùng thêm ---
    private static final String USER_ADDED_FILE_PATH = System.getProperty("user.home") + File.separator + "my_dictionary_added_words.txt";
    private static final File USER_ADDED_FILE = new File(USER_ADDED_FILE_PATH); // Tạo đối tượng File tĩnh
    /**
     * Xóa một từ khỏi file lưu trữ từ người dùng thêm. // Mô tả chức năng xóa.
     * File gốc trong resources KHÔNG bị ảnh hưởng. // Nhấn mạnh file nào bị ảnh hưởng.
     *
     * @param wordToDelete Từ vựng cần xóa. // Mô tả tham số.
     * @throws IOException Nếu có lỗi khi đọc/ghi file. // Mô tả ngoại lệ.
     * @return true nếu từ được tìm thấy và xóa khỏi file, false nếu từ không có trong file hoặc file trống. // Mô tả giá trị trả về.
     */
    public static boolean deleteWordFromFile(String wordToDelete) throws IOException { // Phương thức static xóa từ khỏi file.
        if (wordToDelete == null || wordToDelete.trim().isEmpty()) { // Kiểm tra từ đầu vào có hợp lệ không.
            return false; // Trả về false nếu từ rỗng.
        }
        String targetWord = wordToDelete.trim(); // Loại bỏ khoảng trắng của từ cần xóa.
        // Đối tượng File USER_ADDED_FILE đã được khai báo static ở đầu class UserWordFileManager.

        if (!USER_ADDED_FILE.exists() || USER_ADDED_FILE.length() == 0) { // Kiểm tra nếu file không tồn tại hoặc rỗng.
            System.out.println("User added dictionary file not found or is empty. Nothing to delete: " + USER_ADDED_FILE_PATH); // Báo cáo.
            return false; // Không có gì để xóa.
        }

        List<String> linesToKeep = new ArrayList<>(); // Danh sách tạm để lưu các dòng cần giữ lại.
        boolean wordFoundAndDeleted = false; // Cờ báo hiệu từ cần xóa có được tìm thấy và bỏ qua không.

        // Bước 1: Đọc file, bỏ qua dòng chứa từ cần xóa // Comment bước 1.
        // Sử dụng FileInputStream và InputStreamReader để đảm bảo encoding UTF-8
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(USER_ADDED_FILE), StandardCharsets.UTF_8))) { // Đọc file thêm.
            String line; // Biến lưu nội dung từng dòng.
            while ((line = reader.readLine()) != null) { // Vòng lặp đọc từng dòng.
                String trimmedLine = line.trim(); // Trim dòng đọc được để phân tích.
                if (!trimmedLine.isEmpty()) { // Chỉ xử lý dòng không rỗng.
                    // Cần phân tích dòng để lấy từ, giống như khi load // Logic phân tích dòng để lấy từ.
                    // Giả định format là "từ **unclassified**..."
                    int starIndex = trimmedLine.indexOf("**"); // Tìm dấu phân cách "**".
                    String wordInLine = ""; // Biến lưu từ trong dòng.

                    if (starIndex != -1) { // Nếu tìm thấy "**".
                        wordInLine = trimmedLine.substring(0, starIndex).trim(); // Lấy phần trước "**" làm từ.
                    } else { // Nếu không tìm thấy "**".
                        wordInLine = trimmedLine; // Coi cả dòng là từ (trường hợp lỗi định dạng file thêm?).
                    }

                    // So sánh từ trong dòng (case-insensitive) với từ cần xóa // Thực hiện so sánh từ.
                    if (wordInLine.equalsIgnoreCase(targetWord)) { // So sánh không phân biệt hoa thường.
                        // Tìm thấy dòng cần xóa, không thêm dòng này vào list linesToKeep // Bỏ qua dòng cần xóa.
                        wordFoundAndDeleted = true; // Đánh dấu đã tìm thấy.
                        System.out.println("Found and skipping line for deletion: " + trimmedLine); // Báo cáo.
                    } else {
                        // Dòng này không phải dòng cần xóa, giữ lại // Giữ lại dòng.
                        linesToKeep.add(line); // Thêm dòng *gốc* (bao gồm cả khoảng trắng đầu cuối) vào list để giữ lại.
                    }
                } else {
                    linesToKeep.add(line); // Giữ lại dòng trống để duy trì cấu trúc file.
                }
            }
        } catch (IOException e) { // Bắt lỗi IO khi đọc file.
            System.err.println("Lỗi khi đọc file để xóa từ: " + USER_ADDED_FILE_PATH + " - " + e.getMessage()); // In lỗi.
            throw new IOException("Failed to read file for deletion: " + e.getMessage(), e); // Ném lại lỗi đọc.
        }

        // Nếu từ cần xóa không có trong file, thoát sớm // Kiểm tra kết quả đọc file.
        if (!wordFoundAndDeleted) {
            System.out.println("Word '" + targetWord + "' not found in user added file for deletion."); // Báo cáo.
            return false; // Trả về false vì không tìm thấy từ để xóa.
        }

        // Bước 2: Ghi lại toàn bộ nội dung còn lại vào file (ghi đè file cũ) // Comment bước 2.
        // Nếu linesToKeep rỗng, điều này sẽ tạo ra một file trống // Lưu ý nếu file trống.
        // Sử dụng FileOutputStream và OutputStreamWriter để đảm bảo encoding UTF-8 khi ghi lại
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(USER_ADDED_FILE, false), StandardCharsets.UTF_8))) { // Ghi đè file cũ (false).
            for (int i = 0; i < linesToKeep.size(); i++) { // Lặp qua các dòng cần giữ lại.
                writer.write(linesToKeep.get(i)); // Ghi dòng.
                if (i < linesToKeep.size() - 1) { // Nếu không phải dòng cuối cùng.
                    writer.newLine(); // Thêm xuống dòng.
                }
            }
            System.out.println("Rewritten user added dictionary file after deletion: " + USER_ADDED_FILE_PATH); // Báo cáo đã ghi lại.

        } catch (IOException e) { // Bắt lỗi IO khi ghi file.
            System.err.println("Lỗi khi ghi lại file sau khi xóa từ: " + USER_ADDED_FILE_PATH + " - " + e.getMessage()); // In lỗi.
            // Lỗi này có thể để lại file ở trạng thái không mong muốn. Cần xử lý cẩn thận hơn trong ứng dụng thật. // LƯU Ý VỀ RỦI RO.
            throw new IOException("Failed to rewrite file after deletion: " + e.getMessage(), e); // Ném lại lỗi ghi.
        }

        return true; // Trả về true vì từ đã được tìm thấy và xóa khỏi file.
    }
    // Constructor private để ngăn tạo instance (vì các phương thức là static)
    private UserWordFileManager() {
        // private constructor
    }

    /**
     * Đảm bảo file lưu trữ từ người dùng thêm và thư mục chứa nó tồn tại.
     * Nên gọi khi khởi động ứng dụng hoặc trước khi thao tác ghi/xóa lần đầu.
     */
    public static void ensureFileExists() {
        File parentDir = USER_ADDED_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // Tạo thư mục nếu chưa có
        }
        if (!USER_ADDED_FILE.exists()) {
            try {
                USER_ADDED_FILE.createNewFile();
                System.out.println("Created empty user added dictionary file: " + USER_ADDED_FILE_PATH);
            } catch (IOException e) {
                System.err.println("Could not create user added dictionary file: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
                // Ném một RuntimeException để báo hiệu lỗi nghiêm trọng khi khởi tạo file
                throw new RuntimeException("Failed to create user added dictionary file", e);
            }
        }
    }


    /**
     * Đọc tất cả các dòng từ file lưu trữ từ người dùng thêm.
     *
     * @return Danh sách các dòng (chuỗi định nghĩa thô) từ file. Trả về danh sách rỗng nếu file không tồn tại, rỗng hoặc có lỗi đọc.
     */
    public static List<String> readAllLines() {
        List<String> lines = new ArrayList<>();
        if (!USER_ADDED_FILE.exists() || USER_ADDED_FILE.length() == 0) {
            System.out.println("User added dictionary file not found or is empty: " + USER_ADDED_FILE_PATH);
            return lines; // Trả về danh sách rỗng
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(USER_ADDED_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line); // Thêm dòng gốc vào list
            }
            System.out.println("Read " + lines.size() + " lines from user added dictionary file.");
        } catch (IOException e) {
            System.err.println("Error reading user added dictionary file: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
            // Có thể ném ngoại lệ hoặc chỉ ghi log tùy mức độ nghiêm trọng
            // throw new RuntimeException("Failed to read user added dictionary file", e);
        }
        return lines;
    }


    /**
     * Ghi thêm một dòng mới vào cuối file lưu trữ từ người dùng thêm.
     *
     * @param lineToAdd Dòng văn bản cần thêm.
     * @throws IOException Nếu có lỗi khi ghi file.
     */
    public static void appendLineToFile(String lineToAdd) throws IOException {
        if (lineToAdd == null || lineToAdd.trim().isEmpty()) {
            return; // Không ghi dòng rỗng
        }
        // Đảm bảo file tồn tại trước khi ghi (có thể gọi ensureFileExists() ở đây hoặc khi khởi động app)
        // ensureFileExists(); // Tùy chọn: gọi ở đây nếu muốn đảm bảo 100% file tồn tại trước mỗi lần ghi

        try (FileWriter fw = new FileWriter(USER_ADDED_FILE, true); // true: append mode
             BufferedWriter bw = new BufferedWriter(fw)) {
            // Chỉ thêm xuống dòng nếu file đã có nội dung
            if (USER_ADDED_FILE.exists() && USER_ADDED_FILE.length() > 0) { // Kiểm tra file tồn tại VÀ có nội dung
                bw.newLine(); // Thêm một ký tự xuống dòng trước khi ghi dòng mới
            }
            bw.write(lineToAdd);
            System.out.println("Appended line to user added dictionary file: " + USER_ADDED_FILE_PATH + ": " + lineToAdd);
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi vào file từ điển thêm: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
            throw new IOException("Failed to append line to user added file: " + e.getMessage(), e);
        }
    }

    /**
     * Ghi lại toàn bộ danh sách dòng vào file lưu trữ từ người dùng thêm (ghi đè file cũ).
     * Dùng sau khi xóa một dòng nào đó.
     *
     * @param linesToKeep Danh sách các dòng cần ghi vào file.
     * @throws IOException Nếu có lỗi khi ghi file.
     */
    public static void overwriteFile(List<String> linesToKeep) throws IOException {
        // Đảm bảo file tồn tại trước khi ghi (có thể gọi ensureFileExists() ở đây hoặc khi khởi động app)
        // ensureFileExists(); // Tùy chọn: gọi ở đây

        try (FileWriter fw = new FileWriter(USER_ADDED_FILE, false); // false: overwrite mode
             BufferedWriter bw = new BufferedWriter(fw)) {

            for (int i = 0; i < linesToKeep.size(); i++) {
                bw.write(linesToKeep.get(i));
                if (i < linesToKeep.size() - 1) {
                    bw.newLine(); // Thêm xuống dòng giữa các dòng, nhưng không ở cuối cùng
                }
            }
            System.out.println("Overwritten user added dictionary file with " + linesToKeep.size() + " lines.");

        } catch (IOException e) {
            System.err.println("Lỗi khi ghi lại file từ điển thêm: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
            throw new IOException("Failed to overwrite user added file: " + e.getMessage(), e);
        }
    }

}
