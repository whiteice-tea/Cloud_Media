package com.cloudmedia.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cloudmedia.model.dto.ProgressRequest;
import com.cloudmedia.model.vo.ProgressVO;
import com.cloudmedia.service.GuestSessionService;
import com.cloudmedia.service.VideoProgressService;
import com.cloudmedia.util.ApiResponse;

@RestController
@RequestMapping("/api/video/progress")
public class VideoProgressController {

    private final VideoProgressService videoProgressService;
    private final GuestSessionService guestSessionService;

    public VideoProgressController(VideoProgressService videoProgressService, GuestSessionService guestSessionService) {
        this.videoProgressService = videoProgressService;
        this.guestSessionService = guestSessionService;
    }

    @GetMapping("/{videoId}")
    public ApiResponse<ProgressVO> getProgress(@PathVariable("videoId") String videoId, HttpServletRequest request) {
        String guestId = guestSessionService.requireGuestId(request);
        int seconds = videoProgressService.getProgressSeconds(guestId, videoId);
        return ApiResponse.ok(new ProgressVO(seconds));
    }

    @PostMapping("/{videoId}")
    public ApiResponse<Void> reportProgress(@PathVariable("videoId") String videoId,
                                            @Valid @RequestBody ProgressRequest request,
                                            HttpServletRequest servletRequest) {
        String guestId = guestSessionService.requireGuestId(servletRequest);
        videoProgressService.upsertProgress(guestId, videoId, request.getProgressSeconds());
        return ApiResponse.ok();
    }
}
