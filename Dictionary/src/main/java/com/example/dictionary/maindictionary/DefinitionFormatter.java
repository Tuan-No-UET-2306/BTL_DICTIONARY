package com.example.dictionary.maindictionary;

public class DefinitionFormatter {
    /**
     * Phân tích chuỗi định nghĩa thô từ file cục bộ và format lại cho hiển thị.
     * Dựa trên định dạng: "từ /phiên âm/ \*loại từ\* định nghĩa 1 - định nghĩa 2 - ... \\"
     *
     * @param word Từ vựng.
     * @param rawDefinition Chuỗi định nghĩa thô đọc từ file (phần sau từ).
     * @return Chuỗi đã format.
     */
    public String formatLocalDefinition(String word, String rawDefinition) {
        if (word == null || rawDefinition == null) {
            return "Dữ liệu cục bộ không hợp lệ.";
        }

        StringBuilder formattedText = new StringBuilder();

        // Dòng 1: Từ vựng và Phiên âm
        // Phiên âm nằm giữa hai dấu '/' đầu tiên trong rawDefinition
        int firstSlash = rawDefinition.indexOf('/');
        int secondSlash = rawDefinition.indexOf('/', firstSlash + 1);

        String pronunciation = "";
        String restOfLine = rawDefinition; // Phần còn lại sau khi xử lý phiên âm

        if (firstSlash != -1 && secondSlash != -1) {
            try {
                pronunciation = rawDefinition.substring(firstSlash, secondSlash + 1).trim(); // Bao gồm dấu '/'
                restOfLine = rawDefinition.substring(secondSlash + 1).trim();
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Lỗi phân tích phiên âm cho từ '" + word + "': " + rawDefinition);
                pronunciation = "[Lỗi phân tích phiên âm]";
                restOfLine = rawDefinition; // Giữ nguyên phần còn lại nếu lỗi
            }
        } // Nếu không tìm thấy đủ 2 dấu slash, không có phiên âm từ file.

        formattedText.append(word);
        if (!pronunciation.isEmpty()) {
            formattedText.append(" ").append(pronunciation);
        }
        formattedText.append("\n"); // Xuống dòng


        // Tìm Phần loại từ và Định nghĩa
        // Loại từ nằm sau phiên âm và trước dấu "\*"
        int starIndex = restOfLine.indexOf("\\*"); // Tìm vị trí của "\*" trong phần còn lại

        String partOfSpeech = "";
        String definitionsPart = ""; // Chuỗi chứa các định nghĩa gạch đầu dòng

        if (starIndex != -1) {
            try {
                partOfSpeech = restOfLine.substring(0, starIndex).trim();
                definitionsPart = restOfLine.substring(starIndex + 2).trim(); // Bỏ qua "\*"
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Lỗi phân tích loại từ/định nghĩa cho từ '" + word + "': " + restOfLine);
                partOfSpeech = "[Lỗi phân tích loại từ]";
                definitionsPart = restOfLine; // Giữ nguyên phần còn lại
            }

        } else {
            // Nếu không tìm thấy "\*", coi toàn bộ phần còn lại là phần định nghĩa (không có loại từ?)
            partOfSpeech = ""; // Không có loại từ rõ ràng
            definitionsPart = restOfLine;
        }

        // Xóa bỏ ký tự "\\\\" cuối cùng nếu có (từ định dạng file)
        if (definitionsPart.endsWith("\\\\")) {
            definitionsPart = definitionsPart.substring(0, definitionsPart.length() - 2).trim();
        }
        // Xóa bỏ các ký tự thoát khác nếu cần (ví dụ: "\-") trong định nghĩa
        definitionsPart = definitionsPart.replace("\\-", "-");


        // Dòng 2: Loại từ (ví dụ: danh từ, tính từ)
        if (!partOfSpeech.isEmpty()) {
            formattedText.append(partOfSpeech).append(":\n"); // Thêm dấu ":" và xuống dòng
        }

        // Các dòng tiếp theo: Các định nghĩa gạch đầu dòng
        // Các định nghĩa được phân tách bằng " - "
        if (!definitionsPart.isEmpty()) {
            // Cẩn thận với split(" - ") nếu chuỗi chứa " - " bên trong định nghĩa.
            // Một cách an toàn hơn là tìm vị trí của " - " và split thủ công hoặc dùng regex phức tạp hơn.
            // Nhưng với định dạng file này, split có thể tạm chấp nhận.
            String[] definitions = definitionsPart.split(" - ");
            for (String def : definitions) {
                String trimmedDef = def.trim();
                if (!trimmedDef.isEmpty()) {
                    // Thêm gạch đầu dòng và định nghĩa
                    formattedText.append("- ").append(trimmedDef).append("\n");
                }
            }
        } else if (partOfSpeech.isEmpty() && pronunciation.isEmpty() && formattedText.toString().trim().equals(word)) {
            // Trường hợp chỉ có từ, không có phiên âm, loại từ, định nghĩa chi tiết trong file
            formattedText.append("Không có định nghĩa chi tiết trong file.\n");
        }


        return formattedText.toString().trim(); // Loại bỏ khoảng trắng thừa ở cuối
    }

    /**
     * Phân tích chuỗi JSON phản hồi từ API và format lại cho hiển thị.
     * Cần THAY THẾ logic này bằng thư viện JSON để đáng tin cậy hơn.
     *
     * @param rawJson Chuỗi JSON thô từ API.
     * @return Chuỗi đã format.
     */
    public String formatApiDefinition(String rawJson) {
        // *** CẢNH BÁO: CODE NÀY PHÂN TÍCH CHUỖI THÔ, KHÔNG ĐÁNG TIN CẬY VỚI MỌI PHẢN HỒI API ***
        // HÃY DÙNG THƯ VIỆN JSON NHƯ GSON HOẶC JACKSON!

        StringBuilder result = new StringBuilder();
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return "Không có dữ liệu từ API.";
        }

        try {
            // Cố gắng phát hiện các phản hồi lỗi phổ biến từ API dictionaryapi.dev
            // Đây là cách kiểm tra thô.
            if (rawJson.contains("\"title\":\"No Definitions Found\"") || rawJson.contains("\"message\":")) {
                try {
                    // Cố gắng phân tích thông báo lỗi từ JSON
                    int msgIndex = rawJson.indexOf("\"message\":\"");
                    if (msgIndex != -1) {
                        int startIndex = msgIndex + "\"message\":\"".length();
                        int endIndex = rawJson.indexOf("\"", startIndex);
                        if (endIndex != -1) {
                            String message = rawJson.substring(startIndex, endIndex).replace("\\\"", "\""); // Xử lý thoát ký tự cơ bản
                            result.append("API báo lỗi: ").append(message).append("\n");
                        }
                    }

                    if (result.length() == 0 && rawJson.contains("\"title\":\"")) {
                        int titleIndex = rawJson.indexOf("\"title\":\"");
                        if (titleIndex != -1) {
                            int startIndex = titleIndex + "\"title\":\"".length();
                            int endIndex = rawJson.indexOf("\"", startIndex);
                            if (endIndex != -1) {
                                String title = rawJson.substring(startIndex, endIndex).replace("\\\"", "\""); // Xử lý thoát ký tự cơ bản
                                result.append("API báo lỗi: ").append(title).append("\n");
                            }
                        }
                    }

                    if(result.length() == 0) {
                        result.append("API báo lỗi không rõ nguyên nhân hoặc không tìm thấy định nghĩa.");
                    }

                } catch (Exception e) {
                    System.err.println("Lỗi khi phân tích phản hồi lỗi JSON thô: " + e.getMessage());
                    result.append("API báo lỗi (không phân tích được thông báo chi tiết).");
                }
                return result.toString().trim();
            }


            // Nếu không phải phản hồi lỗi, cố gắng phân tích định nghĩa (cách thô)
            String lowerCaseJson = rawJson.toLowerCase();
            int index = lowerCaseJson.indexOf("\"definition\":\"");
            while (index != -1) {
                int startIndex = index + "\"definition\":\"".length();
                int endIndex = rawJson.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    String definition = rawJson.substring(startIndex, endIndex);
                    // Xử lý các ký tự thoát cơ bản
                    definition = definition.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
                    result.append("- ").append(definition).append("\n");
                    // Tìm lần xuất hiện tiếp theo sau dấu đóng ngoặc kép hiện tại
                    index = lowerCaseJson.indexOf("\"definition\":\"", endIndex + 1);
                } else {
                    break; // Không tìm thấy dấu đóng ngoặc kép, dừng lại
                }
            }

            // Nếu vẫn không tìm thấy định nghĩa sau khi phân tích thô
            if (result.length() == 0) {
                // Cố gắng tìm các trường khác như "meaning", "partOfSpeech" để cung cấp thông tin cơ bản
                if (lowerCaseJson.contains("\"partofspeech\":\"")) {
                    try { // Cố gắng trích xuất loại từ một cách thô sơ
                        int psIndex = lowerCaseJson.indexOf("\"partofspeech\":\"") + "\"partofspeech\":\"".length();
                        int psEndIndex = lowerCaseJson.indexOf("\"", psIndex);
                        if (psEndIndex != -1) {
                            String partOfSpeech = rawJson.substring(psIndex, psEndIndex);
                            result.append("Tìm thấy từ online, nhưng không có định nghĩa chi tiết (Kiểu từ: ").append(partOfSpeech).append(").\n");
                        } else {
                            result.append("Tìm thấy từ online, nhưng không có định nghĩa chi tiết.\n");
                        }
                    } catch (Exception parseE) {
                        System.err.println("Lỗi khi trích xuất partOfSpeech từ JSON thô: " + parseE.getMessage());
                        result.append("Tìm thấy từ online, nhưng không có định nghĩa chi tiết.\n");
                    }
                } else {
                    // Trường hợp không tìm thấy cả definition lẫn partOfSpeech
                    result.append("Không tìm thấy định nghĩa chi tiết từ phản hồi API.");
                }
            }
            return result.toString().trim();

        } catch (Exception e) {
            System.err.println("Lỗi khi phân tích JSON định nghĩa (phương pháp chuỗi thô): " + e.getMessage());
            e.printStackTrace();
            return "Lỗi khi phân tích định nghĩa từ phản hồi API.";
        }

         /*
         // *** VÍ DỤ CÁCH PHÂN TÍCH BẰNG Gson (nếu đã thêm dependency) ***
         // Đây là cách xử lý JSON chính xác và đáng tin cậy hơn nhiều!
         // Cần import com.google.gson.* và thêm dependency Gson vào project.
         // HÃY THAY THẾ TOÀN BỘ HÀM formatApiDefinition bằng code tương tự như sau:
         //
         // try {
         //     Gson gson = new Gson();
         //     JsonArray jsonArray = gson.fromJson(rawJson, JsonArray.class);
         //     StringBuilder gsonResult = new StringBuilder();
         //
         //     if (jsonArray != null && jsonArray.size() > 0) {
         //         JsonObject entry = jsonArray.get(0).getAsJsonObject();
         //         // Lấy phiên âm từ API nếu có
         //         if (entry.has("phonetics")) {
         //              JsonArray phonetics = entry.getAsJsonArray("phonetics");
         //              if (phonetics != null && phonetics.size() > 0) {
         //                   for (int k = 0; k < phonetics.size(); k++) {
         //                        JsonObject phonetic = phonetics.get(k).getAsJsonObject();
         //                        if (phonetic.has("text")) {
         //                             gsonResult.append(" /").append(phonetic.get("text").getAsString()).append("/ ");
         //                             break; // Lấy phiên âm đầu tiên
         //                        }
         //                   }
         //              }
         //         }
         //
         //         JsonArray meanings = entry.getAsJsonArray("meanings");
         //         if (meanings != null && meanings.size() > 0) {
         //             for (int i = 0; i < meanings.size(); i++) {
         //                 JsonObject meaning = meanings.get(i).getAsJsonObject();
         //                 String partOfSpeech = meaning.has("partOfSpeech") ? meaning.get("partOfSpeech").getAsString() : "N/A";
         //                 gsonResult.append("\n").append(partOfSpeech).append(":\n"); // Xuống dòng cho loại từ
         //
         //                 JsonArray definitions = meaning.getAsJsonArray("definitions");
         //                 if (definitions != null && definitions.size() > 0) {
         //                     for (int j = 0; j < definitions.size(); j++) {
         //                         JsonObject definitionObj = definitions.get(j).getAsJsonObject();
         //                         if (definitionObj.has("definition")) {
         //                             String definition = definitionObj.get("definition").getAsString();
         //                             gsonResult.append("- ").append(definition).append("\n");
         //                             // Thêm ví dụ nếu có
         //                             // if (definitionObj.has("example")) {
         //                             //      gsonResult.append("  Ví dụ: ").append(definitionObj.get("example").getAsString()).append("\n");
         //                             // }
         //                         }
         //                     }
         //                 }
         //             }
         //         } else {
         //              gsonResult.append("Không tìm thấy nghĩa chi tiết từ API.");
         //         }
         //     } else {
         //         // Có thể API trả về mảng rỗng hoặc object lỗi khác
         //         return "Không tìm thấy dữ liệu từ điển cho từ này trong phản hồi API.";
         //     }
         //
         //     return gsonResult.toString().trim();
         //
         // } catch (Exception e) {
         //     System.err.println("Lỗi khi phân tích JSON định nghĩa (sử dụng Gson): " + e.getMessage());
         //     e.printStackTrace();
         //     return "Lỗi khi phân tích định nghĩa từ phản hồi API.";
         // }
         */
    }
}
