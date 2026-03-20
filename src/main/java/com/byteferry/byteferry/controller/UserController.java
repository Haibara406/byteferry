package com.byteferry.byteferry.controller;

import com.byteferry.byteferry.model.entity.User;
import com.byteferry.byteferry.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取自己的个人资料
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getMyProfile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userService.getProfile(userId);
        return ResponseEntity.ok(buildFullProfile(user));
    }

    /**
     * 查看他人资料（脱敏）
     */
    @GetMapping("/profile/{username}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable String username) {
        try {
            User user = userService.getProfileByUsername(username);
            return ResponseEntity.ok(buildPublicProfile(user));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 修改个人资料
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(Authentication auth, @RequestBody Map<String, String> body) {
        Long userId = (Long) auth.getPrincipal();
        try {
            User user = userService.updateProfile(userId, body.get("username"), body.get("gender"));
            return ResponseEntity.ok(buildFullProfile(user));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 上传/更换头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(Authentication auth, @RequestParam("file") MultipartFile file) {
        Long userId = (Long) auth.getPrincipal();
        try {
            String avatarUrl = userService.uploadAvatar(userId, file);
            return ResponseEntity.ok(Map.of("avatar", avatarUrl));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 更换邮箱
     */
    @PostMapping("/email/change")
    public ResponseEntity<Map<String, Object>> changeEmail(Authentication auth, @RequestBody Map<String, String> body) {
        Long userId = (Long) auth.getPrincipal();
        String newEmail = body.get("email");
        String code = body.get("code");
        if (newEmail == null || code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email and code are required");
        }
        try {
            userService.changeEmail(userId, newEmail.trim(), code.trim());
            return ResponseEntity.ok(Map.of("message", "邮箱更换成功"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private Map<String, Object> buildFullProfile(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("avatar", user.getAvatar());
        map.put("gender", user.getGender().name());
        map.put("emailBound", user.isEmailBound());
        map.put("createdAt", user.getCreatedAt().toString());
        return map;
    }

    private Map<String, Object> buildPublicProfile(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("avatar", user.getAvatar());
        map.put("gender", user.getGender().name());
        return map;
    }
}
