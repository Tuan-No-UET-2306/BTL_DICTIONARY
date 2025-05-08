package com.example.dictionary.maindictionary;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
// quản lý việc đọc và ghi dữ liệu vào  một tệp tin để lưu từ vựng do ng dùng thêm
public class UserWordFileManager {
    //tạo ra và lưu trữ một chuỗi String chứa đường dẫn đầy đủ
    private static final String USER_ADDED_FILE_PATH = System.getProperty("user.home")
            + File.separator + "my_dictionary_added_words.txt";// separator: đường dẫn đầy đủ
    private static final File USER_ADDED_FILE = new File(USER_ADDED_FILE_PATH);
    public static boolean deleteWordFromFile(String tuCanXoa) throws IOException {
        if (tuCanXoa == null || tuCanXoa.trim().isEmpty()) {
            return false;
        }
        String targetWord = tuCanXoa.trim();
        if (!USER_ADDED_FILE.exists() || USER_ADDED_FILE.length() == 0) {//.exists là ktra file có tồn tại k
            System.out.println("Người dùng đã thêm tệp từ điển không tìm thấy hoặc tệp này trống. Không có gì để xóa: " + USER_ADDED_FILE_PATH);
            return false;
        }

        List<String> linesToKeep = new ArrayList<>();
        boolean wordFoundAndDeleted = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(USER_ADDED_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) {
                    int starIndex = trimmedLine.indexOf("**");
                    String wordInLine = "";

                    if (starIndex != -1) {
                        wordInLine = trimmedLine.substring(0, starIndex).trim();
                    } else {
                        wordInLine = trimmedLine;
                    }

                    if (wordInLine.equalsIgnoreCase(targetWord)) {
                        wordFoundAndDeleted = true;
                        System.out.println("Đã tìm thấy và bỏ qua dòng để xóa: " + trimmedLine);
                    } else {
                        linesToKeep.add(line);
                    }
                } else {
                    linesToKeep.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đọc file để xóa từ: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
            throw new IOException("Không thể đọc tệp để xóa: " + e.getMessage(), e);
        }

        if (!wordFoundAndDeleted) {
            System.out.println("Từ '" + targetWord + "' không tìm thấy trong tệp người dùng thêm để xóa.");
            return false;
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(USER_ADDED_FILE, false), StandardCharsets.UTF_8))) {
            for (int i = 0; i < linesToKeep.size(); i++) {
                writer.write(linesToKeep.get(i));
                if (i < linesToKeep.size() - 1) {
                    writer.newLine();
                }
            }
            System.out.println("Đã viết lại tệp từ điển do người dùng thêm vào sau khi xóa: " + USER_ADDED_FILE_PATH);

        } catch (IOException e) {
            System.err.println("Lỗi khi ghi lại file sau khi xóa từ: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());

            throw new IOException("Không thể ghi lại tệp sau khi xóa: " + e.getMessage(), e);
        }

        return true;
    }

    private UserWordFileManager() {}
// đảm bảo file chứa các từ ng dùng thêm tồn tại
    public static void ensureFileExists() {
        File thuMucGoc = USER_ADDED_FILE.getParentFile();
        if (thuMucGoc != null && !thuMucGoc.exists()) {// néu file ch tồn tại
            thuMucGoc.mkdirs();
        }
        if (!USER_ADDED_FILE.exists()) {
            try {
                USER_ADDED_FILE.createNewFile();
                System.out.println("Đã tạo tệp từ điển trống do người dùng thêm vào: " + USER_ADDED_FILE_PATH);
            } catch (IOException e) {// xử lý khi k tạo được ( thiếu quyền)
                System.err.println("Không thể tạo tệp từ điển do người dùng thêm vào: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
                throw new RuntimeException("Không tạo được tệp từ điển do người dùng thêm vào", e);
            }
        }
    }

    public static List<String> readAllLines() {
        List<String> lines = new ArrayList<>();
        if (!USER_ADDED_FILE.exists() || USER_ADDED_FILE.length() == 0) {
            System.out.println("Không tìm thấy tệp từ điển do người dùng thêm hoặc tệp này trống: " + USER_ADDED_FILE_PATH);
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(USER_ADDED_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            System.out.println("Read " + lines.size() + "các dòng từ tệp từ điển được người dùng thêm vào.");
        } catch (IOException e) {
            System.err.println("Lỗi khi đọc tệp từ điển do người dùng thêm vào: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
        }
        return lines;
    }

    public static void appendLineToFile(String lineToAdd) throws IOException {
        if (lineToAdd == null || lineToAdd.trim().isEmpty()) {
            return;
        }

        try (FileWriter fw = new FileWriter(USER_ADDED_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            if (USER_ADDED_FILE.exists() && USER_ADDED_FILE.length() > 0) {
                bw.newLine();
            }
            bw.write(lineToAdd);
            System.out.println("Thêm dòng vào tệp từ điển do người dùng thêm vào: " + USER_ADDED_FILE_PATH + ": " + lineToAdd);
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi vào file từ điển thêm: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
            throw new IOException("Không thể thêm dòng vào tệp người dùng đã thêm: " + e.getMessage(), e);
        }
    }
// ghi lại toàn bộ nội dung file ng dùng thêm ( ghi đè tệp tin)
    public static void overwriteFile(List<String> linesToKeep) throws IOException {
        try (FileWriter fw = new FileWriter(USER_ADDED_FILE, false);
             BufferedWriter bw = new BufferedWriter(fw)) {

            for (int i = 0; i < linesToKeep.size(); i++) {
                bw.write(linesToKeep.get(i));
                if (i < linesToKeep.size() - 1) {
                    bw.newLine();
                }
            }
            System.out.println("Ghi đè tệp từ điển đã thêm của người dùng " + linesToKeep.size() + " lines.");

        } catch (IOException e) {
            System.err.println("Lỗi khi ghi lại file từ điển thêm: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
            throw new IOException("Không ghi đè được tệp do người dùng thêm vào: " + e.getMessage(), e);
        }
    }
}