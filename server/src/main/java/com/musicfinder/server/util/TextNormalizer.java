package com.musicfinder.server.util;

import java.text.Normalizer;

public class TextNormalizer {

    // "Có Chắc Yêu Là Đây" -> "co chac yeu la day"
    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String s = input.trim().toLowerCase();
        s = s.replace('đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}", ""); // xóa dấu
        s = s.replaceAll("[^\\p{L}\\p{N}\\s]", " "); // bỏ ký tự đặc biệt
        s = s.replaceAll("\\s+", " ").trim(); // gom khoảng trắng
        return s;
    }
}