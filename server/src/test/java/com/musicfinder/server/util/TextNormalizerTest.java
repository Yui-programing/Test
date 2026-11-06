package com.musicfinder.server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    @Test
    void boDauTiengViet() {
        assertEquals("co chac yeu la day", TextNormalizer.normalize("Có Chắc Yêu Là Đây"));
    }

    @Test
    void daKhongDauThiGiuNguyen() {
        assertEquals("co chac yeu la day", TextNormalizer.normalize("co chac yeu la day"));
    }

    @Test
    void thuaKhoangTrangVaKyTuDacBiet() {
        assertEquals("son tung m tp", TextNormalizer.normalize("  Sơn   Tùng M-TP!! "));
    }

    @Test
    void nullHoacRong() {
        assertEquals("", TextNormalizer.normalize(null));
        assertEquals("", TextNormalizer.normalize("   "));
    }
}