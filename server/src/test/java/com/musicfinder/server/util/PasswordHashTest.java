package com.musicfinder.server.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PasswordHashTest {
    @Test
    void hashMatKhauThanhCong() {
        String rawPassword = "mySecretPassword123";
        String hash = PasswordHash.hashPassword(rawPassword);
        // 1. Hash không được rỗng
        assertNotNull(hash);
        assertFalse(hash.isBlank());
        // 2. Chuỗi hash BCrypt phải bắt đầu bằng $2a$ (hoặc $2b$)
        assertTrue(hash.startsWith("$2a$"), "Chuỗi băm phải là định dạng BCrypt");
        System.out.println(hash);
    }

    @Test
    void haiLanHashCungMatKhauPhaiKhacNhau_DoSaltNgauNhien() {
        String rawPassword = "password123";
        String hash1 = PasswordHash.hashPassword(rawPassword);
        String hash2 = PasswordHash.hashPassword(rawPassword);
        // BCrypt tự sinh Salt ngẫu nhiên mỗi lần, nên 2 hash phải khác nhau
        assertNotEquals(hash1, hash2, "2 lần hash cùng 1 mật khẩu phải khác nhau để chống Rainbow Table");
    }

    @Test
    void xacThucMatKhauDung() {
        String rawPassword = "correct_password";
        String hash = PasswordHash.hashPassword(rawPassword);
        // Nhập đúng mật khẩu -> verify phải trả về true
        assertTrue(PasswordHash.verifyPassword("correct_password", hash));
    }

    @Test
    void xacThucMatKhauSai() {
        String rawPassword = "correct_password";
        String hash = PasswordHash.hashPassword(rawPassword);
        // Nhập sai mật khẩu -> verify phải trả về false
        assertFalse(PasswordHash.verifyPassword("wrong_password", hash));
        assertFalse(PasswordHash.verifyPassword("", hash));
        assertFalse(PasswordHash.verifyPassword(null, hash));
    }

    @Test
    void hashMatKhauNullHoacRong_NemNgoaiLe() {
        // Mật khẩu null hoặc khoảng trắng phải ném IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> PasswordHash.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordHash.hashPassword("   "));
    }
}
