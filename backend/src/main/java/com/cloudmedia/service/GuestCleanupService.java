package com.cloudmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.cloudmedia.config.AppProperties;
import com.cloudmedia.mapper.UserMapper;
import com.cloudmedia.model.entity.User;

@Service
public class GuestCleanupService {

    private static final String GUEST_PREFIX = "guest_";

    private final AppProperties appProperties;
    private final UserMapper userMapper;
    private final MediaService mediaService;

    public GuestCleanupService(AppProperties appProperties, UserMapper userMapper, MediaService mediaService) {
        this.appProperties = appProperties;
        this.userMapper = userMapper;
        this.mediaService = mediaService;
    }

    @Scheduled(fixedDelayString = "${app.guest-cleanup-interval-ms:60000}")
    public void cleanupExpiredGuestData() {
        if (!appProperties.isPublicMode()) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(Math.max(appProperties.getGuestTtlMinutes(), 1));
        List<User> expiredGuests = userMapper.selectList(new LambdaQueryWrapper<User>()
                .likeRight(User::getUsername, GUEST_PREFIX)
                .lt(User::getUpdatedAt, cutoff));

        for (User guest : expiredGuests) {
            mediaService.deleteAllByUserId(guest.getId());
            userMapper.deleteById(guest.getId());
        }
    }
}
