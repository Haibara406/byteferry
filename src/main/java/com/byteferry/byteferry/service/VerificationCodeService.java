package com.byteferry.byteferry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private final StringRedisTemplate redisTemplate;

    private static final String CODE_PREFIX = "verify:email:";
    private static final String COOLDOWN_PREFIX = "verify:cooldown:";
    private static final long CODE_TTL_MINUTES = 5;
    private static final long COOLDOWN_SECONDS = 60;

    /**
     * 生成并存储验证码
     */
    public String generateAndStore(String email) {
        // 检查冷却时间（60秒内不可重复发送）
        String cooldownKey = COOLDOWN_PREFIX + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new RuntimeException("验证码发送过于频繁，请60秒后重试");
        }

        String code = generateCode();
        String codeKey = CODE_PREFIX + email;

        redisTemplate.opsForValue().set(codeKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        log.info("验证码已生成: email={}", email);
        return code;
    }

    /**
     * 校验验证码（校验成功后删除）
     */
    public boolean verify(String email, String code) {
        String codeKey = CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode != null && storedCode.equals(code)) {
            redisTemplate.delete(codeKey);
            return true;
        }
        return false;
    }

    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
