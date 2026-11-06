package com.musicfinder.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.musicfinder.server.service.AuthService;
import com.musicfinder.shared.request.payload.ChangePasswordProfilePayload;
import com.musicfinder.shared.request.payload.LoginPayload;
import com.musicfinder.shared.request.payload.RegisterPayload;

public class TestAuth {
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    // ================== TEST VALIDATE ĐĂNG KÝ ==================
    @Test
    void dangKy_PayloadNull_PhaiBaoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(null);
        });
        assertEquals("Dữ liệu đăng kí không được để trống", ex.getMessage());
        
    }

    @Test
    void dangKy_UsernameNgan_PhaiBaoLoi() {
        RegisterPayload payload = new RegisterPayload("ab", "123456", "Nguyen Van A", "a@gmail.com");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(payload);
        });
        assertEquals("Tên đăng nhập phải ít nhất 3 kí tự!", ex.getMessage());
        
    }

    @Test
    void dangKy_PasswordNgan_PhaiBaoLoi() {
        RegisterPayload payload = new RegisterPayload("hiep123", "123", "Truong Hiep", "hiep@gmail.com");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(payload);
        });
        assertEquals("Mật khẩu phải có ít nhất 6 kí tự!", ex.getMessage());
    }

    @Test
    void dangKy_EmailSaiDinhDang_PhaiBaoLoi() {
        RegisterPayload payload = new RegisterPayload("hiep123", "123456", "Truong Hiep", "email-sai-dinh-dang");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(payload);
        });
        assertEquals("Định dạng email không hợp lệ", ex.getMessage());
    }

    // ================== TEST VALIDATE ĐĂNG NHẬP ==================
    @Test
    void dangNhap_DeTrongTaiKhoan_PhaiBaoLoi() {
        LoginPayload payload = new LoginPayload("", "123456");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.login(payload);
        });
        assertEquals("Vui lòng nhập tên đăng nhập hoặc email", ex.getMessage());
    }

    @Test
    void dangNhap_DeTrongMatKhau_PhaiBaoLoi() {
        LoginPayload payload = new LoginPayload("hiep123", "");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.login(payload);
        });
        assertEquals("Vui lòng nhập mật khẩu!", ex.getMessage());
    }

    // ================== TEST VALIDATE ĐỔI MẬT KHẨU ==================
    @Test
    void doiMatKhau_MatKhauMoiNgan_PhaiBaoLoi() {
        ChangePasswordProfilePayload payload = new ChangePasswordProfilePayload("123");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.changePassword("hiep123", payload);
        });
        assertEquals("Mật khẩu mới phải có ít nhất 6 ký tự!", ex.getMessage());
    }
}
