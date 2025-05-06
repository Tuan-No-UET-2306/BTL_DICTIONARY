package com.example.dictionary.maindictionary;

import java.util.Map;
import java.util.Set;

// Class để đóng gói kết quả tải dữ liệu từ điển từ DicDataLoader
public class DictionaryLoadResult {
    private final Map<String, String> allLocalWords;
    private final Set<String> userAddedWords;

    public DictionaryLoadResult(Map<String, String> allLocalWords, Set<String> userAddedWords) {
        this.allLocalWords = allLocalWords;
        this.userAddedWords = userAddedWords;
    }

    public Map<String, String> getAllLocalWords() {
        return allLocalWords;
    }

    public Set<String> getUserAddedWords() {
        return userAddedWords;
    }
}
