package com.example.dictionary.maindictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

    public class DicDataLoader {

        private final String fileTuDien; // Đường dẫn đến file từ điển trong resources

        // Constructor nhận đường dẫn file
        public DicDataLoader(String fileTuDien) {
            this.fileTuDien = fileTuDien;
        }

        /**
         * Đọc file từ điển từ resources và tải dữ liệu.
         * Phân tích từng dòng để lấy từ và toàn bộ phần còn lại của dòng (định nghĩa thô).
         *
         * @return Map chứa từ vựng và chuỗi định nghĩa thô tương ứng. Trả về Map rỗng nếu có lỗi hoặc file trống/không tồn tại.
         */
        public Map<String, String> loadWordsAndRawDefinitions() {
            Map<String, String> localDictionaryRaw = new HashMap<>();
            List<String> wordsList = new ArrayList<>(); // Tạm thời lưu danh sách từ để trả về sau

            try (InputStream is = getClass().getResourceAsStream(fileTuDien);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                if (is == null) {
                    System.err.println("Không tìm thấy file resource: " + fileTuDien);
                    // Ném một ngoại lệ để Controller biết việc tải dữ liệu thất bại
                    throw new IOException("Resource file not found: " + fileTuDien);
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim(); // Xóa khoảng trắng đầu cuối
                    if (!line.isEmpty()) {
                        // --- PHÂN TÍCH DÒNG ĐỂ LẤY TỪ VÀ LƯU TOÀN BỘ CHUỖI THÔ CÒN LẠI ---
                        // Dựa trên định dạng: "từ /phiên âm/ \*loại từ\* định nghĩa \\"
                        // Tìm vị trí của dấu cách hoặc / đầu tiên để tách từ
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
                        String rawDefinitionPart = ""; // Chuỗi chứa phiên âm, loại từ, định nghĩa...

                        if (firstSpaceOrSlashIndex != -1) {
                            word = line.substring(0, firstSpaceOrSlashIndex).trim();
                            rawDefinitionPart = line.substring(firstSpaceOrSlashIndex).trim();
                        } else {
                            // Nếu không tìm thấy dấu cách/slash, coi toàn bộ dòng là từ (trường hợp không có phiên âm/định nghĩa?)
                            word = line.trim();
                            rawDefinitionPart = ""; // Không có phần định nghĩa thô
                        }

                        if (!word.isEmpty()) {
                            // Lưu vào Map TOÀN BỘ CHUỖI THÔ CÒN LẠI
                            localDictionaryRaw.put(word, rawDefinitionPart);
                            // Thêm từ vào danh sách tạm thời (sẽ được sort trong Controller)
                            wordsList.add(word);
                        } else {
                            System.err.println("Bỏ qua dòng không lấy được từ: " + line);
                        }
                        // -----------------------------------------------------------
                    }
                }
                System.out.println("Đã tải " + wordsList.size() + " từ từ file cục bộ.");

            } catch (IOException e) {
                System.err.println("Lỗi khi đọc file từ điển: " + e.getMessage());
                // Ném ngoại lệ để Controller có thể bắt và thông báo cho người dùng
                throw new RuntimeException("Failed to load dictionary data from " + fileTuDien, e);
            }

            // Sau khi đọc xong, lưu danh sách từ vào một biến nội bộ để Controller lấy
            // (Hoặc bạn có thể trả về một Pair/custom object chứa cả Map và List)
            // Cách đơn giản hơn là Controller lấy Map rồi tự tạo List từ keys nếu cần,
            // nhưng giữ List riêng giúp việc sort và hiển thị ListView nhanh hơn.
            // Tạm thời chỉ trả về Map, Controller sẽ tự quản lý List và sorting.
            return localDictionaryRaw;
        }

        // Phương thức này có thể không cần thiết nếu Controller tự lấy keys từ Map
        // public List<String> getWordsList(Map<String, String> loadedData) {
        //     List<String> words = new ArrayList<>(loadedData.keySet());
        //     return words; // List này chưa được sort
        // }
}
