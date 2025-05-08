package com.example.dictionary.maindictionary;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
public class DefinitionFormatter {

    public String formatLocalDefinition(String word, String chuoiVanBan) {
        if (word == null || chuoiVanBan == null) {
            return "Dữ liệu không hợp lệ.";
        }

        StringBuilder thongTinTu = new StringBuilder();

        int tuTimKiem = chuoiVanBan.indexOf('/');
        int loaiTu = chuoiVanBan.indexOf('/', tuTimKiem + 1);

        String phienAm = "";
        String dinhNghia = chuoiVanBan;

        if (tuTimKiem != -1 && loaiTu != -1) {
            try {
                phienAm = chuoiVanBan.substring(tuTimKiem, loaiTu + 1).trim();
                dinhNghia = chuoiVanBan.substring(loaiTu + 1).trim();
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Lỗi phân tích phiên âm cho từ '" + word + "': " + chuoiVanBan);
                phienAm = "[Lỗi phân tích phiên âm]";
                dinhNghia = chuoiVanBan;
            }
        }
        thongTinTu.append(word);
        if (!phienAm.isEmpty()) {
            thongTinTu.append(" ").append(phienAm);
        }
        thongTinTu.append("\n");
        int starIndex = dinhNghia.indexOf("\\*");
        String LoaiTu = "";
        String dinhNghiaChiTiet = "";

        if (starIndex != -1) {
            try {
                LoaiTu = dinhNghia.substring(0, starIndex).trim();
                dinhNghiaChiTiet = dinhNghia.substring(starIndex + 2).trim();
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Lỗi phân tích loại từ/định nghĩa cho từ '" + word + "': " + dinhNghia);
                LoaiTu = "[Lỗi phân tích loại từ]";
                dinhNghiaChiTiet = dinhNghia;
            }

        }
        else {
            LoaiTu = "";
            dinhNghiaChiTiet = dinhNghia;
        }

        if (dinhNghiaChiTiet.endsWith("\\\\")) {
            dinhNghiaChiTiet = dinhNghiaChiTiet.substring(0, dinhNghiaChiTiet.length() - 2).trim();
        }
        dinhNghiaChiTiet = dinhNghiaChiTiet.replace("\\-", "-");
        if (!LoaiTu.isEmpty()) {
            thongTinTu.append(LoaiTu).append(":\n");
        }

        if (!dinhNghiaChiTiet.isEmpty()) {
            String[] definitions = dinhNghiaChiTiet.split(" - ");
            for (String def : definitions) {
                String trimmedDef = def.trim();
                if (!trimmedDef.isEmpty()) {
                    thongTinTu.append("- ").append(trimmedDef).append("\n");
                }
            }
        }
        else if (LoaiTu.isEmpty() && phienAm.isEmpty() && thongTinTu.toString().trim().equals(word)) {
            thongTinTu.append("Không có định nghĩa chi tiết.\n");
        }
        return thongTinTu.toString().trim();
    }

    public String formatChuoi(String chuoiTho) {
        StringBuilder result = new StringBuilder();
        if (chuoiTho == null || chuoiTho.trim().isEmpty()) {
            return "Không có dữ liệu.";
        }
        try {
            JSONArray jsonArray = new JSONArray(chuoiTho);
            if (jsonArray.length() == 0) {
                return "Không tìm thấy định nghĩa.";
            }
            JSONObject entry = jsonArray.getJSONObject(0);
            if (entry.has("title") && "Không tìm thấy định nghĩa.".equals(entry.optString("title"))) {
                result.append("API báo lỗi: ").append(entry.getString("title"));
                if (entry.has("message")) {
                    result.append("\nChi tiết: ").append(entry.getString("message"));
                }
                if (entry.has("resolution")) {
                    result.append("\nKhắc phục: ").append(entry.getString("resolution"));
                }
                return result.toString().trim();
            }
            if (entry.has("message") && !entry.has("word") && !entry.has("meanings")) {
                result.append("API báo lỗi: ").append(entry.getString("message"));
                return result.toString().trim();
            }
            if (entry.has("meanings")) {
                JSONArray meaningsArray = entry.getJSONArray("meanings");
                boolean foundDetailedDefinition = false;
                for (int i = 0; i < meaningsArray.length(); i++) {
                    JSONObject meaning = meaningsArray.getJSONObject(i);
                    String partOfSpeech = "";
                    if (meaning.has("partOfSpeech")) {
                        partOfSpeech = meaning.getString("partOfSpeech");
                        if (result.length() > 0 && result.charAt(result.length()-1) != '\n') {
                            result.append("\n");
                        }
                        result.append("*").append(partOfSpeech).append("*").append(":\n");
                    }
                    if (meaning.has("definitions")) {
                        JSONArray definitionsArray = meaning.getJSONArray("definitions");
                        for (int j = 0; j < definitionsArray.length(); j++) {
                            JSONObject definitionObj = definitionsArray.getJSONObject(j);
                            if (definitionObj.has("definition")) {
                                String definitionText = definitionObj.getString("definition");
                                result.append("- ").append(definitionText).append("\n");
                                foundDetailedDefinition = true;
                            }
                        }
                    }
                }
                if (!foundDetailedDefinition) {
                    if (result.toString().trim().isEmpty()) {
                        result.append("Không tìm thấy định nghĩa chi tiết từ phản hồi API (cấu trúc không như mong đợi).");
                    }
                }
            }
            else {
                if (result.toString().trim().isEmpty()) {
                    result.append("Không tìm thấy thông tin nghĩa của từ trong phản hồi API.");
                }
            }
            if (result.toString().trim().isEmpty()) {
                result.append("Không thể trích xuất thông tin định nghĩa từ API.");
            }
            return result.toString().trim();

        } catch (JSONException e) {
            System.err.println("Lỗi phân tích JSON từ API: " + e.getMessage());
            System.err.println("JSON thô nhận được: " + chuoiTho);
            return "Lỗi khi phân tích định nghĩa từ phản hồi API (JSON không hợp lệ hoặc cấu trúc không đúng).";
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi xử lý phản hồi API: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi không xác định khi xử lý định nghĩa từ API.";
        }
    }
}
