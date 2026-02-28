package com.kkh.shop_1.common.util;

public class ChosungUtils {
    private static final String[] CHOSUNG_LIST = {
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ",
            "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    public static String extract(String word) {
        if (word == null) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            // 한글 가(0xAC00) ~ 힣(0xD7A3) 사이인 경우
            if (ch >= 0xAC00 && ch <= 0xD7A3) {
                int index = (ch - 0xAC00) / (21 * 28);
                result.append(CHOSUNG_LIST[index]);
            } else {
                result.append(ch); // 한글이 아니면(영어, 숫자, 공백 등) 그대로 유지
            }
        }
        return result.toString();
    }
}