package com.example.dictionary.maindictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;
import java.util.List; // <-- Import cần thiết

// Import UserWordFileManager
// import com.example.dictionary.maindictionary.UserWordFileManager; // Nếu cùng package thì không cần import tường minh


public class DicDataLoader {

    private final String resourcePath; // Đường dẫn đến file từ điển gốc trong resources (để đọc)


    // Constructor nhận đường dẫn file Resource (để đọc file gốc)
    public DicDataLoader(String resourcePath) {
        this.resourcePath = resourcePath;
        System.out.println("DataLoader initialized with resource path (for reading): " + resourcePath);

        // Đảm bảo file người dùng thêm tồn tại khi DataLoader được tạo
        try {
            UserWordFileManager.ensureFileExists(); // <-- Gọi phương thức đảm bảo file tồn tại
        } catch (RuntimeException e) {
            System.err.println("Error ensuring user added file exists: " + e.getMessage());
            // Có thể ném tiếp e hoặc chỉ ghi log tùy mức độ nghiêm trọng
            // throw e; // Ném tiếp nếu coi đây là lỗi khởi tạo nghiêm trọng
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
        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            if (is == null) {
                System.err.println("Không tìm thấy file resource gốc: " + resourcePath);
                throw new RuntimeException("Resource file not found: " + resourcePath);
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
            throw new RuntimeException("Failed to load dictionary data from " + resourcePath, e);
        }

        // --- Bước 2: Đọc từ file lưu trữ từ người dùng thêm (gọi UserWordFileManager) ---
        // Lấy tất cả các dòng từ file người dùng thêm
        List<String> userAddedLines = UserWordFileManager.readAllLines(); // <-- Gọi UserWordFileManager để đọc file thêm

        int addedCount = 0;
        // Phân tích từng dòng đọc được từ file người dùng thêm và thêm vào Map
        for (String line : userAddedLines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                // Logic phân tích dòng trong file user_added_words.txt
                // Cần khớp với cách bạn format chuỗi newEntry trong appendWordToFile của UserWordFileManager
                // Giả định format là "từ **unclassified**..."
                int starIndex = trimmedLine.indexOf("**");
                String wordOnly = "";
                String rawDefinitionPart = "";

                if (starIndex != -1) {
                    wordOnly = trimmedLine.substring(0, starIndex).trim();
                    rawDefinitionPart = trimmedLine.substring(starIndex).trim();
                } else {
                    wordOnly = trimmedLine.trim();
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
        System.out.println("Đã tải " + addedCount + " từ từ file thêm.");


        System.out.println("Tổng số từ tải thành công từ cả hai nguồn: " + localDictionaryRaw.size());
        return localDictionaryRaw;
    }

    // --- Các phương thức appendWordToFile và deleteWordFromFile đã bị di chuyển sang UserWordFileManager ---
}