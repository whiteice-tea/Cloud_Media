package com.cloudmedia.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cloudmedia.config.AppProperties;
import com.cloudmedia.model.dto.ProgressRequest;
import com.cloudmedia.model.vo.ProgressVO;
import com.cloudmedia.service.GuestSessionService;
import com.cloudmedia.service.VideoProgressService;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;
import com.cloudmedia.util.ApiResponse;
import com.cloudmedia.util.UserContext;

@RestController
@RequestMapping("/api/video/progress")
public class VideoProgressController {

    private final VideoProgressService videoProgressService;
    private final AppProperties appProperties;
    private final GuestSessionService guestSessionService;

    public VideoProgressController(VideoProgressService videoProgressService, AppProperties appProperties,
                                   GuestSessionService guestSessionService) {
        this.videoProgressService = videoProgressService;
        this.appProperties = appProperties;
        this.guestSessionService = guestSessionService;
    }

    @GetMapping("/{videoId}")
    public ApiResponse<ProgressVO> getProgress(@PathVariable("videoId") Long videoId, HttpServletRequest request) {
        Long userId = resolveCallerUserId(request);
        int seconds = videoProgressService.getProgressSeconds(userId, videoId);
        return ApiResponse.ok(new ProgressVO(seconds));
    }

    @PostMapping("/{videoId}")
    public ApiResponse<Void> reportProgress(@PathVariable("videoId") Long videoId,
                                            @Valid @RequestBody ProgressRequest request,
                                            HttpServletRequest servletRequest) {
        Long userId = resolveCallerUserId(servletRequest);
        videoProgressService.upsertProgress(userId, videoId, request.getProgressSeconds());
        return ApiResponse.ok();
    }

    private Long resolveCallerUserId(HttpServletRequest request) {
        if (appProperties.isPublicMode()) {
            return guestSessionService.requireGuestUserId(request);
        }
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ApiException(ApiCode.UNAUTHORIZED, "unauthorized");
        }
        return userId;
    }
}
