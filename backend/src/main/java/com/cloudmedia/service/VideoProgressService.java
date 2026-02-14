package com.cloudmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import com.cloudmedia.mapper.VideoProgressMapper;
import com.cloudmedia.model.entity.VideoProgress;

@Service
public class VideoProgressService {

    private final VideoProgressMapper videoProgressMapper;
    private final MediaService mediaService;

    public VideoProgressService(VideoProgressMapper videoProgressMapper, MediaService mediaService) {
        this.videoProgressMapper = videoProgressMapper;
        this.mediaService = mediaService;
    }

    public int getProgressSeconds(String guestId, String videoId) {
        mediaService.getOwnedVideoForProgress(guestId, videoId);
        VideoProgress videoProgress = videoProgressMapper.selectOne(new LambdaQueryWrapper<VideoProgress>()
                .eq(VideoProgress::getGuestId, guestId)
                .eq(VideoProgress::getVideoFileId, videoId)
                .last("LIMIT 1"));
        return videoProgress == null ? 0 : Math.max(videoProgress.getProgressSeconds(), 0);
    }

    public void upsertProgress(String guestId, String videoId, int seconds) {
        mediaService.getOwnedVideoForProgress(guestId, videoId);

        int safeSeconds = Math.max(seconds, 0);
        VideoProgress existing = videoProgressMapper.selectOne(new LambdaQueryWrapper<VideoProgress>()
                .eq(VideoProgress::getGuestId, guestId)
                .eq(VideoProgress::getVideoFileId, videoId)
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            VideoProgress progress = new VideoProgress();
            progress.setGuestId(guestId);
            progress.setVideoFileId(videoId);
            progress.setProgressSeconds(safeSeconds);
            progress.setUpdatedAt(now);
            videoProgressMapper.insert(progress);
            return;
        }
        existing.setProgressSeconds(safeSeconds);
        existing.setUpdatedAt(now);
        videoProgressMapper.updateById(existing);
    }
}
