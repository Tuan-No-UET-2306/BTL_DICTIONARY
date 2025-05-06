package com.example.dictionary.maindictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set; // Import Set
import java.util.HashSet; // Import HashSet

public class DicDataLoader {

    private final String resourcePath; // Đường dẫn đến file từ điển gốc trong resources (để đọc)


    // Constructor nhận đường dẫn file Resource (để đọc file gốc)
    public DicDataLoader(String resourcePath) {
        this.resourcePath = resourcePath;
        System.out.println("DataLoader initialized with resource path (for reading): " + resourcePath);

        // Đảm bảo file người dùng thêm tồn tại khi DataLoader được tạo
        try {
            UserWordFileManager.ensureFileExists(); // Gọi phương thức đảm bảo file tồn tại
        } catch (RuntimeException e) {
            System.err.println("Error ensuring user added file exists: " + e.getMessage());
            // Có thể ném tiếp e hoặc chỉ ghi log tùy mức độ nghiêm trọng
            // throw e;
        }
    }

    /**
     * Đọc dữ liệu từ điển từ file resource gốc VÀ file lưu từ người dùng thêm.
     * Phân tích từng dòng để lấy từ và toàn bộ phần còn lại của dòng (định nghĩa thô).
     *
     * @return DictionaryLoadResult chứa Map các từ/định nghĩa thô và Set các từ từ file người dùng thêm.
     *         Trả về kết quả với Map/Set rỗng nếu có lỗi nghiêm trọng hoặc cả hai file đều trống/không tồn tại.
     */
    public DictionaryLoadResult loadWordsAndRawDefinitions() { // <-- Thay đổi kiểu trả về
        Map<String, String> localDictionaryRaw = new HashMap<>();
        Set<String> userAddedWordsSet = new HashSet<>(); // <-- Set để lưu từ người dùng thêm
        int count = 0;

        // --- Bước 1: Đọc từ file resource gốc (chỉ đọc) ---
        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            if (is == null) {
                System.err.println("Không tìm thấy file resource gốc: " + resourcePath);
                // Không ném Exception ở đây để có thể tiếp tục đọc file người dùng thêm
                // throw new RuntimeException("Resource file not found: " + resourcePath); // <-- Bỏ throw này
            } else {
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
                            if (!localDictionaryRaw.containsKey(word)) { // Chỉ thêm nếu từ CHƯA tồn tại
                                localDictionaryRaw.put(word, rawDefinitionPart);
                                count++;
                            } else {
                                // System.out.println("Bỏ qua từ trùng lặp khi tải từ resource gốc: " + word);
                                // Bỏ qua từ trùng trong file gốc, không cần ghi đè
                            }
                        } else {
                            System.err.println("Bỏ qua dòng không lấy được từ từ resource gốc: " + line);
                        }
                    }
                }
                System.out.println("Đã tải " + count + " từ từ file resource gốc.");
            }


        } catch (IOException e) {
            System.err.println("Lỗi khi đọc file từ điển gốc: " + e.getMessage());
            // Vẫn tiếp tục để đọc file người dùng thêm
        }


        // --- Bước 2: Đọc từ file lưu trữ từ người dùng thêm (gọi UserWordFileManager) ---
        // Lấy tất cả các dòng từ file người dùng thêm
        List<String> userAddedLines = UserWordFileManager.readAllLines();

        int addedCount = 0;
        // Phân tích từng dòng đọc được từ file người dùng thêm và thêm vào Map + Set
        for (String line : userAddedLines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                // Logic phân tích dòng trong file user_added_words.txt
                // Cần khớp với cách bạn format chuỗi newEntry trong appendWordToFile
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
                    // Luôn đánh dấu từ này là từ người dùng thêm
                    userAddedWordsSet.add(wordOnly);

                    // Thêm hoặc ghi đè vào map chính
                    // Nếu từ đã có trong map (từ file gốc), sẽ ghi đè định nghĩa bằng định nghĩa từ file người dùng thêm
                    // Nếu từ chưa có, sẽ thêm mới
                    localDictionaryRaw.put(wordOnly, rawDefinitionPart);
                    // Không cần tăng count ở đây vì map.put sẽ xử lý việc này
                    addedCount++;
                } else {
                    System.err.println("Bỏ qua dòng không lấy được từ từ file thêm: " + line);
                }
            }
        }
        System.out.println("Đã tải " + addedCount + " từ từ file thêm.");


        System.out.println("Tổng số từ tải thành công từ cả hai nguồn: " + localDictionaryRaw.size());

        // <-- Trả về đối tượng DictionaryLoadResult
        return new DictionaryLoadResult(localDictionaryRaw, userAddedWordsSet);
    }
}