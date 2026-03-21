package com.byteferry.byteferry.service;

import com.byteferry.byteferry.enums.UploadEnum;
import com.byteferry.byteferry.model.entity.User;
import com.byteferry.byteferry.repository.UserRepository;
import com.byteferry.byteferry.util.FileUploadUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileUploadUtils fileUploadUtils;
    private final VerificationCodeService verificationCodeService;

    private static final String DEFAULT_AVATAR = "/images/default-avatar.jpg";

    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public User getProfileByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 修改基本信息（用户名、性别）
     */
    public User updateProfile(Long userId, String username, String gender) {
        User user = getProfile(userId);

        if (username != null && !username.isBlank()) {
            String trimmed = username.trim();
            if (trimmed.length() < 3 || trimmed.length() > 50) {
                throw new RuntimeException("用户名长度需为 3-50 个字符");
            }
            if (!trimmed.equals(user.getUsername()) && userRepository.existsByUsername(trimmed)) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(trimmed);
        }

        if (gender != null && !gender.isBlank()) {
            try {
                user.setGender(User.Gender.valueOf(gender.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的性别值，可选: MALE, FEMALE, OTHER, UNKNOWN");
            }
        }

        return userRepository.save(user);
    }

    /**
     * 上传/更换头像
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = getProfile(userId);

        // 上传新头像到 MinIO
        String newAvatarUrl = fileUploadUtils.upload(UploadEnum.USER_AVATAR, file);

        // 删除旧头像（非默认头像）
        String oldAvatar = user.getAvatar();
        if (oldAvatar != null && !oldAvatar.equals(DEFAULT_AVATAR)) {
            fileUploadUtils.deleteByUrl(oldAvatar);
        }

        user.setAvatar(newAvatarUrl);
        userRepository.save(user);
        return newAvatarUrl;
    }

    /**
     * 更换邮箱（需验证码）
     */
    public void changeEmail(Long userId, String newEmail, String code) {
        if (!verificationCodeService.verify(newEmail, code)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("该邮箱已被其他账号使用");
        }

        User user = getProfile(userId);
        user.setEmail(newEmail);
        userRepository.save(user);
    }
}
