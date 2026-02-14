package com.cloudmedia.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.cloudmedia.config.GuestProperties;

@Service
public class GuestCleanupService {

    private final MediaService mediaService;
    private final GuestProperties guestProperties;

    public GuestCleanupService(MediaService mediaService, GuestProperties guestProperties) {
        this.mediaService = mediaService;
        this.guestProperties = guestProperties;
    }

    @Scheduled(fixedDelayString = "${guest.cleanup-interval-ms}")
    public void cleanupExpiredGuestData() {
        if (guestProperties.getCleanupIntervalMs() <= 0) {
            return;
        }
        mediaService.cleanupExpiredMedia();
    }
}
