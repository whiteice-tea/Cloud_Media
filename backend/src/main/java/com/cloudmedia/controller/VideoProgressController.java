package com.cloudmedia.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cloudmedia.model.dto.ProgressRequest;
import com.cloudmedia.model.vo.ProgressVO;
import com.cloudmedia.service.VideoProgressService;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;
import com.cloudmedia.util.ApiResponse;
import com.cloudmedia.util.UserContext;

@RestController
@RequestMapping("/api/video/progress")
public class VideoProgressController {

    private final VideoProgressService videoProgressService;

    public VideoProgressController(VideoProgressService videoProgressService) {
        this.videoProgressService = videoProgressService;
    }

    @GetMapping("/{videoId}")
    public ApiResponse<ProgressVO> getProgress(@PathVariable("videoId") Long videoId) {
        Long userId = requireUserId();
        int seconds = videoProgressService.getProgressSeconds(userId, videoId);
        return ApiResponse.ok(new ProgressVO(seconds));
    }

    @PostMapping("/{videoId}")
    public ApiResponse<Void> reportProgress(@PathVariable("videoId") Long videoId, @Valid @RequestBody ProgressRequest request) {
        Long userId = requireUserId();
        videoProgressService.upsertProgress(userId, videoId, request.getProgressSeconds());
        return ApiResponse.ok();
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ApiException(ApiCode.UNAUTHORIZED, "unauthorized");
        }
        return userId;
    }
}
