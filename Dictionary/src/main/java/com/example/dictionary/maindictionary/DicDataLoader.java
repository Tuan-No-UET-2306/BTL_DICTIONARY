package com.example.dictionary.maindictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
// tải dữ liệu từ các nguồn cục bộ lên ứng dụng
public class DicDataLoader {
    private final String duongDan; // biến lưu đường dẫn file gốc

    // constructor của class DicDataLoader
    public DicDataLoader(String duongDan) {
        this.duongDan = duongDan;        // 2. đảm bảo file ng dùng thêm tồn tại
        try {
            UserWordFileManager.ensureFileExists();
        } catch (RuntimeException e) {
            // 3. Xử lý lỗi (nếu có) khi đảm bảo file tồn tại
            System.err.println("Lỗi đảm bảo tệp người dùng thêm vào tồn tại: " + e.getMessage());
        }
    }
// đọc và xử lý dữ liệu từ 2 nguồn file khác nhau
    public DicLoadResult loadWordsAndRawDefinitions() {
        Map<String, String> tuDienTho = new HashMap<>();
        Set<String> userAddedWordsSet = new HashSet<>();
        int initialCount = 0;// đếm số từ tải từ thư viên

        System.out.println("Đang cố gắng tải từ điển gốc từ: " + duongDan);
        try (InputStream is = getClass().getResourceAsStream(duongDan);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            if (is == null) {
                System.err.println("Không tìm thấy file resource gốc: " + duongDan + ". Sẽ chỉ tải từ file người dùng thêm (nếu có).");
            } else {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // tách word với định nghĩa
                    if (!line.isEmpty()) {
                        int firstKTorGC = -1;
                        int khoangTrong = line.indexOf(' ');
                        int gachCheo = line.indexOf('/');

                        if (khoangTrong != -1 && gachCheo != -1) firstKTorGC = Math.min(khoangTrong, gachCheo);
                        else if (khoangTrong != -1) firstKTorGC = khoangTrong;
                        else if (gachCheo != -1) firstKTorGC = gachCheo;

                        String word = "";
                        String dinhNghiaTho = "";

                        if (firstKTorGC != -1) {
                            word = line.substring(0, firstKTorGC).trim();
                            dinhNghiaTho = line.substring(firstKTorGC).trim();
                        } else {
                            word = line.trim();
                        }

                        if (!word.isEmpty()) {
                            if (!tuDienTho.containsKey(word)) {// chỉ thêm từ nếu ch có trong Map
                                tuDienTho.put(word, dinhNghiaTho);
                                initialCount++;

                            }
                        } else {
                            System.err.println("Bỏ qua dòng không lấy được từ từ resource gốc: " + line);
                        }
                    }
                }
                System.out.println("Đã tải " + initialCount + " từ từ file resource gốc.");
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đọc file từ điển gốc (" + duongDan + "): " + e.getMessage());
        } catch (NullPointerException e) {// trả về NULL
            System.err.println("Lỗi NullPointerException khi đọc file resource gốc: " + duongDan + ". Có thể file không tồn tại hoặc đường dẫn sai. " + e.getMessage());
        }

        System.out.println("Đang cố gắng tải các từ do người dùng thêm vào...");
        // lấy tất cả các dòng từ file ng dùng
        List<String> userAddedLines = UserWordFileManager.readAllLines();
        int addedCount = 0;
        for (String line : userAddedLines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                int starIndex = trimmedLine.indexOf("**");
                String wordOnly = "";
                String rawDefinitionPart = "";

                if (starIndex != -1) {
                    wordOnly = trimmedLine.substring(0, starIndex).trim();
                    rawDefinitionPart = trimmedLine.substring(starIndex).trim();
                } else {
                    wordOnly = trimmedLine.trim();
                }
                if (!wordOnly.isEmpty()) {
                    userAddedWordsSet.add(wordOnly);
                    tuDienTho.put(wordOnly, rawDefinitionPart);
                    addedCount++;
                } else {
                    System.err.println("Bỏ qua dòng không lấy được từ từ file thêm: " + line);
                }
            }
        }
        System.out.println("Đã tải " + addedCount + " từ từ file thêm.");
        System.out.println("Tổng số từ trong bộ nhớ (tuDienTho): " + tuDienTho.size());
        return new DicLoadResult(tuDienTho, userAddedWordsSet);
    }
}
