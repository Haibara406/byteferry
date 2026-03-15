package com.byteferry.byteferry.controller;

import com.byteferry.byteferry.model.entity.User;
import com.byteferry.byteferry.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password required");
        }
        try {
            User user = authService.register(username.trim(), password);
            return ResponseEntity.ok(Map.of("message", "Registered", "username", user.getUsername()));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password required");
        }
        try {
            String token = authService.login(username.trim(), password);
            return ResponseEntity.ok(Map.of("token", token, "username", username.trim()));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Long userId = (Long) auth.getPrincipal();
        User user = authService.getUserById(userId);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "createdAt", user.getCreatedAt().toString()
        ));
    }
}
