package com.musicfinder.server.service;

import com.musicfinder.server.model.Users;
import com.musicfinder.server.repository.UserRepository;
import com.musicfinder.server.util.PasswordHash;
import com.musicfinder.server.util.DtoMapper;
import com.musicfinder.shared.request.payload.ChangePasswordProfilePayload;
import com.musicfinder.shared.request.payload.EditProfilePayload;
import com.musicfinder.shared.request.payload.LoginPayload;
import com.musicfinder.shared.request.payload.RegisterPayload;
import com.musicfinder.shared.response.payload.UserData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthService {
    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Đăng ký
    public UserData register(RegisterPayload payload) {
        if (payload == null)
            throw new IllegalArgumentException("Dữ liệu đăng kí không được để trống");
        if (payload.username() == null || payload.username().trim().length() < 3)
            throw new IllegalArgumentException("Tên đăng nhập phải ít nhất 3 kí tự!");
        if (payload.password() == null || payload.password().trim().length() < 6)
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 kí tự!");
        if (payload.email() != null && !payload.email().isBlank()
                && !payload.email().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ");
        }

        String username = payload.username().trim();
        String email = (payload.email() != null && !payload.email().isBlank() ? payload.email().trim() : null);

        // Kiểm tra user
        if (userRepository.existsByUsername(username))
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tài, vui lòng chọn tên khác!");
        if (email != null && userRepository.existByEmail(email))
            throw new IllegalArgumentException("Email này đã được sử dụng!");

        String hashedPassword = PasswordHash.hashPassword(payload.password());
        Users newUser = Users.builder()
                .username(username)
                .email(email)
                .passwordHash(hashedPassword)
                .fullName(payload.fullName() != null ? payload.fullName().trim() : username)
                .avatarUrl(null)
                .build();
        Users saveUser = userRepository.save(newUser);
        log.info("Người dùng đã đăng kí thành công: {}", saveUser.getUsername());
        return DtoMapper.toUserData(saveUser);
    }

    // Đăng nhập
    public UserData login(LoginPayload payload) {
        if (payload == null || payload.usernameOrEmail() == null || payload.usernameOrEmail().isBlank())
            throw new IllegalArgumentException("Vui lòng nhập tên đăng nhập hoặc email");
        if (payload.password() == null || payload.password().isEmpty())
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu!");
        String str = payload.usernameOrEmail().trim();

        // Tìm tài khoản
        Users user = userRepository.findByUsernameOrEmail(str)
                // Kiểm tra opt.empty()
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác"));
        ;
        if (!PasswordHash.verifyPassword(payload.password(), user.getPasswordHash()))
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác");
        return DtoMapper.toUserData(user);
    }

    // Lấy thông tin profile
    public UserData getProfile(String username) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại" + username));
        return DtoMapper.toUserData(user);
    }

    // Cập nhật hồ sơ
    public UserData updateProfile(String username, EditProfilePayload payload) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại" + username));
        if (payload.fullName() != null && !payload.fullName().isBlank()) {
            user.setFullName(payload.fullName().trim());
        }
        if (payload.email() != null && !payload.email().isBlank()) {
            String newEmail = payload.email();
            if (userRepository.existByEmail(newEmail) && !newEmail.equals(user.getEmail()))
                throw new IllegalArgumentException("Email đã tồn tại!");
            user.setEmail(newEmail);
        }
        if (payload.avatarUrl() != null) {
            user.setAvatarUrl(payload.avatarUrl().trim());
        }
        userRepository.update(user);
        return DtoMapper.toUserData(user);
    }

    // Đổi mật khẩu
    public boolean changePassword(String username, ChangePasswordProfilePayload payload) {
        String newPassword = payload.password();
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng " + username));
        String newHash = PasswordHash.hashPassword(newPassword.trim());

        // cập nhật vào DB
        boolean updated = userRepository.updatePassword(user.getUserId(), newHash);
        if (updated)
            log.info("Người dùng [{}] đã đổi mật khẩu thành công ", username);
        return updated;
    }

}
