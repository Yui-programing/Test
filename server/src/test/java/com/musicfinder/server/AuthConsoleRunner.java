package com.musicfinder.server;

import java.util.Scanner;

import com.musicfinder.server.config.DatabaseConfig;
import com.musicfinder.server.service.AuthService;
import com.musicfinder.shared.request.payload.ChangePasswordProfilePayload;
import com.musicfinder.shared.request.payload.EditProfilePayload;
import com.musicfinder.shared.request.payload.LoginPayload;
import com.musicfinder.shared.request.payload.RegisterPayload;
import com.musicfinder.shared.response.payload.UserData;

public class AuthConsoleRunner {

    private static final AuthService authService = new AuthService();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("      CHƯƠNG TRÌNH TEST THAO TÁC AUTH (DB THẬT)   ");
        System.out.println("==================================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("👉 Chọn thao tác (0-5): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleRegister();
                case "2" -> handleLogin();
                case "3" -> handleGetProfile();
                case "4" -> handleUpdateProfile();
                case "5" -> handleChangePassword();
                case "0" -> {
                    System.out.println("\n👋 Đang thoát chương trình và đóng kết nối DB...");
                    running = false;
                }
                default -> System.out.println("⚠️ Lựa chọn không hợp lệ, vui lòng chọn từ 0 đến 5!");
            }
            System.out.println();
        }

        DatabaseConfig.shutdown();
        scanner.close();
        System.out.println("✅ Đã đóng kết nối CSDL thành công!");
    }

    private static void printMenu() {
        System.out.println("--------------------------------------------------");
        System.out.println("1. [ĐĂNG KÝ]      Tạo tài khoản mới vào DB");
        System.out.println("2. [ĐĂNG NHẬP]    Kiểm tra đăng nhập");
        System.out.println("3. [XEM PROFILE]  Tra cứu thông tin người dùng");
        System.out.println("4. [SỬA PROFILE]  Cập nhật họ tên, email, avatar");
        System.out.println("5. [ĐỔI MẬT KHẨU] Cập nhật mật khẩu mới vào DB");
        System.out.println("0. Thoát");
        System.out.println("--------------------------------------------------");
    }

    private static void handleRegister() {
        System.out.println("\n--- [1] ĐĂNG KÝ TÀI KHOẢN ---");
        System.out.print("Nhập username (tối thiểu 3 ký tự): ");
        String username = scanner.nextLine().trim();

        System.out.print("Nhập mật khẩu (tối thiểu 6 ký tự): ");
        String password = scanner.nextLine().trim();

        System.out.print("Nhập họ và tên: ");
        String fullName = scanner.nextLine().trim();

        System.out.print("Nhập email (ví dụ: test@gmail.com, có thể để trống): ");
        String email = scanner.nextLine().trim();

        try {
            RegisterPayload payload = new RegisterPayload(username, password, fullName, email.isEmpty() ? null : email);
            UserData result = authService.register(payload);

            System.out.println("\n🎉 ĐĂNG KÝ THÀNH CÔNG VÀ ĐÃ LƯU VÀO DB!");
            printUserData(result);
        } catch (Exception e) {
            System.out.println("\n❌ ĐĂNG KÝ THẤT BẠI: " + e.getMessage());
        }
    }

    private static void handleLogin() {
        System.out.println("\n--- [2] ĐĂNG NHẬP ---");
        System.out.print("Nhập username hoặc email: ");
        String usernameOrEmail = scanner.nextLine().trim();

        System.out.print("Nhập mật khẩu: ");
        String password = scanner.nextLine().trim();

        try {
            LoginPayload payload = new LoginPayload(usernameOrEmail, password);
            UserData result = authService.login(payload);

            System.out.println("\n🎉 ĐĂNG NHẬP THÀNH CÔNG!");
            printUserData(result);
        } catch (Exception e) {
            System.out.println("\n❌ ĐĂNG NHẬP THẤT BẠI: " + e.getMessage());
        }
    }

    private static void handleGetProfile() {
        System.out.println("\n--- [3] XEM PROFILE ---");
        System.out.print("Nhập username cần xem: ");
        String username = scanner.nextLine().trim();

        try {
            UserData result = authService.getProfile(username);
            System.out.println("\n🎉 LẤY THÔNG TIN THÀNH CÔNG TỪ DB:");
            printUserData(result);
        } catch (Exception e) {
            System.out.println("\n❌ KHÔNG TÌM THẤY: " + e.getMessage());
        }
    }

    private static void handleUpdateProfile() {
        System.out.println("\n--- [4] CẬP NHẬT PROFILE ---");
        System.out.print("Nhập username cần cập nhật: ");
        String username = scanner.nextLine().trim();

        System.out.print("Nhập họ tên mới: ");
        String newFullName = scanner.nextLine().trim();

        System.out.print("Nhập email mới: ");
        String newEmail = scanner.nextLine().trim();

        System.out.print("Nhập URL avatar mới (hoặc để trống): ");
        String newAvatar = scanner.nextLine().trim();

        try {
            EditProfilePayload payload = new EditProfilePayload(
                    newFullName,
                    newEmail,
                    newAvatar.isEmpty() ? null : newAvatar
            );
            UserData result = authService.updateProfile(username, payload);

            System.out.println("\n🎉 CẬP NHẬT THÀNH CÔNG VÀO DB!");
            printUserData(result);
        } catch (Exception e) {
            System.out.println("\n❌ CẬP NHẬT THẤT BẠI: " + e.getMessage());
        }
    }

    private static void handleChangePassword() {
        System.out.println("\n--- [5] ĐỔI MẬT KHẨU ---");
        System.out.print("Nhập username cần đổi mật khẩu: ");
        String username = scanner.nextLine().trim();

        System.out.print("Nhập mật khẩu mới (tối thiểu 6 ký tự): ");
        String newPassword = scanner.nextLine().trim();

        try {
            ChangePasswordProfilePayload payload = new ChangePasswordProfilePayload(newPassword);
            boolean ok = authService.changePassword(username, payload);

            if (ok) {
                System.out.println("\n🎉 ĐỔI MẬT KHẨU THÀNH CÔNG VÀ ĐÃ BĂM MÃ HOÁ LƯU VÀO DB!");
            } else {
                System.out.println("\n⚠️ Không thể cập nhật mật khẩu.");
            }
        } catch (Exception e) {
            System.out.println("\n❌ ĐỔI MẬT KHẨU THẤT BẠI: " + e.getMessage());
        }
    }

    private static void printUserData(UserData data) {
        System.out.println("  - Username  : " + data.userName());
        System.out.println("  - Họ và tên : " + data.fullName());
        System.out.println("  - Email     : " + data.email());
        System.out.println("  - Avatar URL: " + data.avatarUrl());
    }
}
