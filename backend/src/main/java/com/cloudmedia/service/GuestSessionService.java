package com.cloudmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.cloudmedia.mapper.UserMapper;
import com.cloudmedia.model.entity.User;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;

@Service
public class GuestSessionService {

    public static final String HEADER_GUEST_ID = "X-Guest-Id";
    private static final Pattern GUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final String GUEST_PREFIX = "guest_";

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public GuestSessionService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Long requireGuestUserId(HttpServletRequest request) {
        String guestId = request.getHeader(HEADER_GUEST_ID);
        if (!StringUtils.hasText(guestId)) {
            throw new ApiException(ApiCode.BAD_REQUEST, "missing X-Guest-Id");
        }
        if (!GUEST_ID_PATTERN.matcher(guestId).matches()) {
            throw new ApiException(ApiCode.BAD_REQUEST, "invalid X-Guest-Id");
        }

        String username = GUEST_PREFIX + guestId;
        LocalDateTime now = LocalDateTime.now();

        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (existing != null) {
            touch(existing.getId(), now);
            return existing.getId();
        }

        User guest = new User();
        guest.setUsername(username);
        guest.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
        guest.setEmail(null);
        guest.setCreatedAt(now);
        guest.setUpdatedAt(now);

        try {
            userMapper.insert(guest);
            return guest.getId();
        } catch (DuplicateKeyException e) {
            User retry = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, username)
                    .last("LIMIT 1"));
            if (retry == null) {
                throw new ApiException(ApiCode.INTERNAL_ERROR, "failed to create guest session");
            }
            touch(retry.getId(), now);
            return retry.getId();
        }
    }

    public void touch(Long userId) {
        touch(userId, LocalDateTime.now());
    }

    public void touch(Long userId, LocalDateTime now) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getUpdatedAt, now));
    }
}
