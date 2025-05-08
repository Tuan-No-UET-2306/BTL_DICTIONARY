package com.example.dictionary.maindictionary;
import java.util.Map;
import java.util.Set;
//đóng gói và vận chuyển kết quả của quá trình tải dữ liệu
// từ điển thực hiện bởi class DicDataLoader.
public class DicLoadResult {
    public final Map<String, String> allWords;
    public final Set<String> userAddedWords;

    public DicLoadResult(Map<String, String> allWords, Set<String> userAddedWords) {
        this.allWords = allWords;
        this.userAddedWords = userAddedWords;
    }
}
