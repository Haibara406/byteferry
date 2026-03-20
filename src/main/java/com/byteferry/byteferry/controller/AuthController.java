package com.byteferry.byteferry.controller;

import com.byteferry.byteferry.config.JwtUtil;
import com.byteferry.byteferry.model.entity.User;
import com.byteferry.byteferry.service.AuthService;
import com.byteferry.byteferry.service.EmailService;
import com.byteferry.byteferry.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final VerificationCodeService verificationCodeService;
    private final EmailService emailService;

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public ResponseEntity<Map<String, Object>> sendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        try {
            String code = verificationCodeService.generateAndStore(email.trim());
            emailService.sendVerificationCode(email.trim(), code);
            return ResponseEntity.ok(Map.of("message", "验证码已发送"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 邮箱验证码注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        String username = body.get("username");
        String password = body.get("password");
        if (email == null || code == null || username == null || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email, code, username, password are required");
        }
        try {
            User user = authService.register(email.trim(), code.trim(), username.trim(), password);
            return ResponseEntity.ok(Map.of("message", "注册成功", "username", user.getUsername()));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 用户名 + 密码登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password required");
        }
        try {
            User user = authService.login(username.trim(), password);
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("username", user.getUsername());
            result.put("emailBound", user.isEmailBound());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * 邮箱 + 验证码登录
     */
    @PostMapping("/login/email")
    public ResponseEntity<Map<String, Object>> loginByEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        if (email == null || code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email and code are required");
        }
        try {
            String token = authService.loginByEmail(email.trim(), code.trim());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * 老用户绑定邮箱
     */
    @PostMapping("/bind-email")
    public ResponseEntity<Map<String, Object>> bindEmail(Authentication auth, @RequestBody Map<String, String> body) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String email = body.get("email");
        String code = body.get("code");
        if (email == null || code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email and code are required");
        }
        try {
            Long userId = (Long) auth.getPrincipal();
            authService.bindEmail(userId, email.trim(), code.trim());
            return ResponseEntity.ok(Map.of("message", "邮箱绑定成功，请重新登录"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Long userId = (Long) auth.getPrincipal();
        User user = authService.getUserById(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("avatar", user.getAvatar());
        result.put("gender", user.getGender().name());
        result.put("emailBound", user.isEmailBound());
        result.put("createdAt", user.getCreatedAt().toString());
        return ResponseEntity.ok(result);
    }
}
