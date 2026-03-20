package com.byteferry.byteferry.service;

import com.byteferry.byteferry.config.JwtUtil;
import com.byteferry.byteferry.model.entity.User;
import com.byteferry.byteferry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final VerificationCodeService verificationCodeService;

    /**
     * 邮箱验证码注册（新流程）
     */
    public User register(String email, String code, String username, String password) {
        // 校验验证码
        if (!verificationCodeService.verify(email, code)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("该邮箱已被注册");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new RuntimeException("用户名长度需为 3-50 个字符");
        }
        if (password.length() < 6) {
            throw new RuntimeException("密码长度至少 6 位");
        }

        User user = User.builder()
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(password))
                .emailBound(true)
                .build();
        return userRepository.save(user);
    }

    /**
     * 用户名 + 密码登录，返回 User 以便 controller 获取 emailBound
     */
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        return user;
    }

    /**
     * 邮箱 + 验证码登录
     */
    public String loginByEmail(String email, String code) {
        if (!verificationCodeService.verify(email, code)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("该邮箱未注册"));

        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    /**
     * 老用户绑定邮箱
     */
    public void bindEmail(Long userId, String email, String code) {
        if (!verificationCodeService.verify(email, code)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("该邮箱已被其他账号绑定");
        }

        User user = getUserById(userId);
        user.setEmail(email);
        user.setEmailBound(true);
        userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
