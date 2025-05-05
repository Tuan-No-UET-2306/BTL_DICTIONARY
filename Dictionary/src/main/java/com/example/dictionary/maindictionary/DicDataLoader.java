package com.example.dictionary.maindictionary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList; // <-- Import cần thiết
import java.util.HashMap;
import java.util.List;      // <-- Import cần thiết
import java.util.Map;

public class DicDataLoader {

    private final String fileDuongDan; // Đường dẫn đến file từ điển gốc trong resources (để đọc)

    // --- Đường dẫn cố định để ghi file (Bạn có thể thay đổi vị trí này) ---
    private static final String USER_ADDED_FILE_PATH = System.getProperty("user.home") + File.separator + "my_dictionary_added_words.txt";
    // File.separator tự động dùng '\' trên Windows và '/' trên Linux/macOS.

    // Constructor nhận đường dẫn file Resource (để đọc file gốc)
    public DicDataLoader(String fileDuongDan) {
        this.fileDuongDan = fileDuongDan;
        System.out.println("DataLoader initialized with resource path (for reading): " + fileDuongDan);
        System.out.println("User-added words will be saved to (for writing): " + USER_ADDED_FILE_PATH);

        // Tùy chọn: Tạo file rỗng nếu nó chưa tồn tại khi ứng dụng khởi động lần đầu
        File userAddedFile = new File(USER_ADDED_FILE_PATH);
        if (!userAddedFile.exists()) {
            try {
                // Đảm bảo thư mục chứa file tồn tại trước khi tạo file
                File parentDir = userAddedFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs(); // Tạo thư mục nếu chưa có
                }
                userAddedFile.createNewFile();
                System.out.println("Created empty user added dictionary file: " + USER_ADDED_FILE_PATH);
            } catch (IOException e) {
                System.err.println("Could not create user added dictionary file: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
                // Ứng dụng vẫn chạy, nhưng thêm/xóa từ sẽ báo lỗi nếu file không tồn tại và không tạo được.
            }
        }
    }

    /**
     * Đọc dữ liệu từ điển từ file resource gốc VÀ file lưu từ người dùng thêm.
     * Phân tích từng dòng để lấy từ và toàn bộ phần còn lại của dòng (định nghĩa thô).
     *
     * @return Map chứa từ vựng và chuỗi định nghĩa thô tương ứng từ cả hai nguồn.
     *         Trả về Map rỗng nếu có lỗi nghiêm trọng hoặc cả hai file đều trống/không tồn tại.
     */
    public Map<String, String> loadWordsAndRawDefinitions() {
        Map<String, String> localDictionaryRaw = new HashMap<>();
        int count = 0;

        // --- Bước 1: Đọc từ file resource gốc (chỉ đọc) ---
        try (InputStream is = getClass().getResourceAsStream(fileDuongDan);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            if (is == null) {
                System.err.println("Không tìm thấy file resource gốc: " + fileDuongDan);
                throw new RuntimeException("Resource file not found: " + fileDuongDan);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    // Logic phân tích dòng để lấy từ và phần định nghĩa thô
                    int firstSpaceOrSlashIndex = -1;
                    int spaceIndex = line.indexOf(' ');
                    int slashIndex = line.indexOf('/');

                    if (spaceIndex != -1 && slashIndex != -1) {
                        firstSpaceOrSlashIndex = Math.min(spaceIndex, slashIndex);
                    } else if (spaceIndex != -1) {
                        firstSpaceOrSlashIndex = spaceIndex;
                    } else if (slashIndex != -1) {
                        firstSpaceOrSlashIndex = slashIndex;
                    }

                    String word = "";
                    String rawDefinitionPart = "";

                    if (firstSpaceOrSlashIndex != -1) {
                        word = line.substring(0, firstSpaceOrSlashIndex).trim();
                        rawDefinitionPart = line.substring(firstSpaceOrSlashIndex).trim();
                    } else {
                        word = line.trim();
                        rawDefinitionPart = "";
                    }

                    if (!word.isEmpty()) {
                        if (!localDictionaryRaw.containsKey(word)) {
                            localDictionaryRaw.put(word, rawDefinitionPart);
                            count++;
                        } else {
                            System.out.println("Bỏ qua từ trùng lặp khi tải từ resource gốc: " + word);
                        }
                    } else {
                        System.err.println("Bỏ qua dòng không lấy được từ từ resource gốc: " + line);
                    }
                }
            }
            System.out.println("Đã tải " + count + " từ từ file resource gốc.");

        } catch (IOException e) {
            System.err.println("Lỗi khi đọc file từ điển gốc: " + e.getMessage());
            throw new RuntimeException("Failed to load dictionary data from " + fileDuongDan, e);
        }

        // --- Bước 2: Đọc từ file lưu trữ từ người dùng thêm (đọc/ghi) ---
        File userAddedFile = new File(USER_ADDED_FILE_PATH);
        if (userAddedFile.exists() && userAddedFile.length() > 0) { // Chỉ đọc nếu file tồn tại và không rỗng
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(userAddedFile), StandardCharsets.UTF_8))) {
                String line;
                int addedCount = 0;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        // Logic phân tích dòng trong file user_added_words.txt
                        // Cần khớp với cách bạn format chuỗi newEntry trong appendWordToFile
                        // Giả định format là "từ **unclassified**..."
                        int starIndex = line.indexOf("**");
                        String wordOnly = "";
                        String rawDefinitionPart = "";

                        if (starIndex != -1) {
                            wordOnly = line.substring(0, starIndex).trim();
                            rawDefinitionPart = line.substring(starIndex).trim();
                        } else {
                            wordOnly = line.trim();
                            rawDefinitionPart = ""; // Trường hợp lỗi định dạng hoặc file cũ
                        }


                        if (!wordOnly.isEmpty()) {
                            if (!localDictionaryRaw.containsKey(wordOnly)) { // Chỉ thêm nếu từ CHƯA tồn tại trong Map (cả từ gốc và đã thêm)
                                localDictionaryRaw.put(wordOnly, rawDefinitionPart); // Sử dụng định nghĩa từ file thêm
                                count++; // Tổng số từ tăng
                                addedCount++; // Đếm số từ thêm từ file này
                            } else {
                                System.out.println("Bỏ qua từ trùng lặp khi tải từ file thêm: " + wordOnly);
                            }
                        } else {
                            System.err.println("Bỏ qua dòng không lấy được từ từ file thêm: " + line);
                        }
                    }
                }
                System.out.println("Đã tải " + addedCount + " từ từ file thêm: " + USER_ADDED_FILE_PATH);
            } catch (IOException e) {
                System.err.println("Lỗi khi đọc file từ điển thêm: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
                // Có thể hiển thị cảnh báo cho người dùng thay vì ném exception nặng
            }
        } else {
            System.out.println("File từ điển thêm không tồn tại hoặc rỗng: " + USER_ADDED_FILE_PATH);
        }

        System.out.println("Tổng số từ tải thành công từ cả hai nguồn: " + localDictionaryRaw.size()); // In tổng số từ trong map
        return localDictionaryRaw;
    }

    /**
     * Thêm một từ và định nghĩa mặc định vào cuối file lưu trữ từ người dùng thêm.
     *
     * @param word Từ vựng cần thêm.
     * @param defaultRawDefinition Phần định nghĩa thô mặc định (ví dụ: "\*\*unclassified\*\* No definition added.\\").
     * @throws IOException Nếu có lỗi khi ghi file.
     */
    public void appendWordToFile(String word, String defaultRawDefinition) throws IOException {
        if (word == null || word.trim().isEmpty()) {
            throw new IllegalArgumentException("Word cannot be null or empty.");
        }
        // Định dạng dòng mới theo cấu trúc file lưu trữ từ người dùng
        String newEntry = word.trim() + " " + defaultRawDefinition.trim();

        File file = new File(USER_ADDED_FILE_PATH);
        // Đảm bảo thư mục chứa file tồn tại nếu cần (Đã làm trong constructor, nhưng làm lại ở đây cũng an toàn)
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileWriter fw = new FileWriter(file, true); // true: append mode
             BufferedWriter bw = new BufferedWriter(fw)) {
            if (file.exists() && file.length() > 0) { // Kiểm tra file tồn tại VÀ có nội dung
                bw.newLine(); // Thêm một ký tự xuống dòng trước khi ghi từ mới, đảm bảo từ mới ở dòng riêng.
            }
            bw.write(newEntry);
            System.out.println("Đã thêm vào file: " + USER_ADDED_FILE_PATH + ": " + newEntry);
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi vào file từ điển thêm: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
            throw new IOException("Failed to append word to user added file: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa một từ khỏi file lưu trữ từ người dùng thêm.
     * File gốc trong resources KHÔNG bị ảnh hưởng.
     *
     * @param wordToDelete Từ vựng cần xóa.
     * @throws IOException Nếu có lỗi khi đọc/ghi file.
     * @return true nếu từ được tìm thấy và xóa, false nếu từ không có trong file.
     */
    public boolean deleteWordFromFile(String wordToDelete) throws IOException {
        if (wordToDelete == null || wordToDelete.trim().isEmpty()) {
            return false; // Không xóa từ rỗng
        }
        String targetWord = wordToDelete.trim();
        File userAddedFile = new File(USER_ADDED_FILE_PATH);

        if (!userAddedFile.exists() || userAddedFile.length() == 0) {
            System.out.println("File thêm không tồn tại hoặc rỗng. Không có gì để xóa: " + USER_ADDED_FILE_PATH);
            return false; // File không có, không có gì để xóa
        }

        List<String> linesToKeep = new ArrayList<>();
        boolean wordFoundAndDeleted = false;

        // Bước 1: Đọc file, bỏ qua dòng chứa từ cần xóa
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(userAddedFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) {
                    // Cần phân tích dòng để lấy từ, giống như khi load
                    int starIndex = trimmedLine.indexOf("**");
                    String wordInLine = "";
                    if (starIndex != -1) {
                        wordInLine = trimmedLine.substring(0, starIndex).trim();
                    } else {
                        wordInLine = trimmedLine; // Có thể chỉ có từ
                    }

                    // So sánh từ trong dòng (case-insensitive)
                    if (wordInLine.equalsIgnoreCase(targetWord)) {
                        // Tìm thấy dòng cần xóa, không thêm dòng này vào list linesToKeep
                        wordFoundAndDeleted = true; // Đánh dấu đã tìm thấy và bỏ qua
                        System.out.println("Tìm thấy và bỏ qua dòng để xóa: " + trimmedLine);
                    } else {
                        // Dòng này không phải dòng cần xóa, giữ lại
                        linesToKeep.add(line); // Thêm dòng gốc (có thể có khoảng trắng đầu cuối) vào list
                    }
                } else {
                    // Giữ lại dòng trống để format file không thay đổi quá nhiều
                    linesToKeep.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đọc file để xóa từ: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
            throw new IOException("Failed to read file for deletion: " + e.getMessage(), e); // Ném lại lỗi đọc
        }

        // Nếu từ cần xóa không có trong file, thoát sớm
        if (!wordFoundAndDeleted) {
            System.out.println("Từ '" + targetWord + "' không được tìm thấy trong file thêm để xóa.");
            return false;
        }

        // Bước 2: Ghi lại toàn bộ nội dung còn lại vào file (ghi đè file cũ)
        // Nếu linesToKeep rỗng, điều này sẽ tạo ra một file trống
        try (FileWriter fw = new FileWriter(userAddedFile, false); // false: overwrite mode
             BufferedWriter bw = new BufferedWriter(fw)) {

            for (int i = 0; i < linesToKeep.size(); i++) {
                bw.write(linesToKeep.get(i));
                if (i < linesToKeep.size() - 1) {
                    bw.newLine(); // Thêm xuống dòng giữa các dòng, nhưng không ở cuối cùng
                }
            }
            System.out.println("Đã ghi lại nội dung (sau khi xóa) vào file: " + USER_ADDED_FILE_PATH);

        } catch (IOException e) {
            System.err.println("Lỗi khi ghi lại file sau khi xóa từ: " + USER_ADDED_FILE_PATH + " - " + e.getMessage());
            // Lỗi này có thể để lại file ở trạng thái không mong muốn. Cần xử lý cẩn thận hơn trong ứng dụng thật.
            throw new IOException("Failed to rewrite file after deletion: " + e.getMessage(), e); // Ném lại lỗi ghi
        }

        return true; // Xóa thành công
    }
}