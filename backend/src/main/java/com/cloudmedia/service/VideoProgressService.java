package com.cloudmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import com.cloudmedia.mapper.VideoProgressMapper;
import com.cloudmedia.model.entity.MediaFile;
import com.cloudmedia.model.entity.VideoProgress;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;

@Service
public class VideoProgressService {

    private final VideoProgressMapper videoProgressMapper;
    private final MediaService mediaService;

    public VideoProgressService(VideoProgressMapper videoProgressMapper, MediaService mediaService) {
        this.videoProgressMapper = videoProgressMapper;
        this.mediaService = mediaService;
    }

    public int getProgressSeconds(Long userId, Long videoId) {
        MediaFile mediaFile = mediaService.getOwnedMedia(userId, videoId);
        if (!MediaService.TYPE_VIDEO.equals(mediaFile.getMediaType())) {
            throw new ApiException(ApiCode.BAD_REQUEST, "media is not video");
        }
        VideoProgress videoProgress = videoProgressMapper.selectOne(new LambdaQueryWrapper<VideoProgress>()
                .eq(VideoProgress::getUserId, userId)
                .eq(VideoProgress::getVideoFileId, videoId)
                .last("LIMIT 1"));
        return videoProgress == null ? 0 : Math.max(videoProgress.getProgressSeconds(), 0);
    }

    public void upsertProgress(Long userId, Long videoId, int seconds) {
        MediaFile mediaFile = mediaService.getOwnedMedia(userId, videoId);
        if (!MediaService.TYPE_VIDEO.equals(mediaFile.getMediaType())) {
            throw new ApiException(ApiCode.BAD_REQUEST, "media is not video");
        }

        int safeSeconds = Math.max(seconds, 0);
        VideoProgress existing = videoProgressMapper.selectOne(new LambdaQueryWrapper<VideoProgress>()
                .eq(VideoProgress::getUserId, userId)
                .eq(VideoProgress::getVideoFileId, videoId)
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            VideoProgress progress = new VideoProgress();
            progress.setUserId(userId);
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
