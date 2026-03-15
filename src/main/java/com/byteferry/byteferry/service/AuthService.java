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

    public User register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new RuntimeException("Username must be 3-50 characters");
        }
        if (password.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .build();
        return userRepository.save(user);
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
