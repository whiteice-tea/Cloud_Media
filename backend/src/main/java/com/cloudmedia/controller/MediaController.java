package com.cloudmedia.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.cloudmedia.model.entity.MediaFile;
import com.cloudmedia.model.vo.MediaItemVO;
import com.cloudmedia.service.MediaService;
import com.cloudmedia.service.WordConvertService;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;
import com.cloudmedia.util.ApiResponse;
import com.cloudmedia.util.UserContext;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private static final int BUFFER_SIZE = 16 * 1024;

    private final MediaService mediaService;
    private final WordConvertService wordConvertService;

    public MediaController(MediaService mediaService, WordConvertService wordConvertService) {
        this.mediaService = mediaService;
        this.wordConvertService = wordConvertService;
    }

    @PostMapping("/upload/video")
    public ApiResponse<Map<String, Long>> uploadVideo(@RequestParam("file") MultipartFile file) {
        Long userId = requireUserId();
        Long id = mediaService.uploadVideo(userId, file);
        return ApiResponse.ok(Map.of("id", id));
    }

    @PostMapping("/upload/doc")
    public ApiResponse<Map<String, Long>> uploadDoc(@RequestParam("file") MultipartFile file) {
        Long userId = requireUserId();
        Long id = mediaService.uploadDoc(userId, file);
        return ApiResponse.ok(Map.of("id", id));
    }

    @GetMapping("/list")
    public ApiResponse<List<MediaItemVO>> list(@RequestParam("type") String type) {
        Long userId = requireUserId();
        return ApiResponse.ok(mediaService.listByType(userId, type));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        Long userId = requireUserId();
        mediaService.deleteOwnedMedia(userId, id);
        return ApiResponse.ok();
    }

    @GetMapping("/stream/video/{id}")
    public void streamVideo(@PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long userId = requireUserId();
        MediaFile mediaFile = mediaService.getOwnedMedia(userId, id);
        if (!MediaService.TYPE_VIDEO.equals(mediaFile.getMediaType())) {
            throw new ApiException(ApiCode.BAD_REQUEST, "media is not video");
        }
        Path filePath = mediaService.resolveStoredPath(mediaFile);
        if (!Files.exists(filePath)) {
            throw new ApiException(ApiCode.NOT_FOUND, "video file not found");
        }

        long fileLength = Files.size(filePath);
        long start = 0L;
        long end = fileLength - 1;
        boolean partial = false;
        String rangeHeader = request.getHeader("Range");
        if (StringUtils.hasText(rangeHeader) && rangeHeader.startsWith("bytes=")) {
            String range = rangeHeader.substring("bytes=".length());
            String[] parts = range.split("-", 2);
            try {
                start = Long.parseLong(parts[0].trim());
                if (parts.length > 1 && StringUtils.hasText(parts[1])) {
                    end = Long.parseLong(parts[1].trim());
                }
                if (start < 0 || end < start || end >= fileLength) {
                    throw new ApiException(ApiCode.BAD_REQUEST, "invalid range");
                }
                partial = true;
            } catch (NumberFormatException e) {
                throw new ApiException(ApiCode.BAD_REQUEST, "invalid range format");
            }
        }

        long contentLength = end - start + 1;
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Type", "video/mp4");
        response.setHeader("Content-Length", String.valueOf(contentLength));
        if (partial) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            raf.seek(start);
            byte[] buffer = new byte[BUFFER_SIZE];
            long remaining = contentLength;
            while (remaining > 0) {
                int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    break;
                }
                response.getOutputStream().write(buffer, 0, read);
                remaining -= read;
            }
            response.flushBuffer();
        }
    }

    @GetMapping("/view/doc/{id}")
    public void viewDoc(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        Long userId = requireUserId();
        MediaFile mediaFile = mediaService.getOwnedMedia(userId, id);
        if (!MediaService.TYPE_DOC.equals(mediaFile.getMediaType())) {
            throw new ApiException(ApiCode.BAD_REQUEST, "media is not doc");
        }

        Path pdfPath;
        if ("pdf".equalsIgnoreCase(mediaFile.getExt())) {
            pdfPath = mediaService.resolveStoredPath(mediaFile);
        } else {
            pdfPath = wordConvertService.ensureDerivedPdf(mediaFile);
        }
        if (!Files.exists(pdfPath)) {
            throw new ApiException(ApiCode.NOT_FOUND, "pdf file not found");
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Type", "application/pdf");
        response.setHeader("Content-Length", String.valueOf(Files.size(pdfPath)));
        Files.copy(pdfPath, response.getOutputStream());
        response.flushBuffer();
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ApiException(ApiCode.UNAUTHORIZED, "unauthorized");
        }
        return userId;
    }
}
