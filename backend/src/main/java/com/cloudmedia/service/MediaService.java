package com.cloudmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.cloudmedia.config.GuestProperties;
import com.cloudmedia.config.StorageProperties;
import com.cloudmedia.mapper.MediaFileMapper;
import com.cloudmedia.mapper.VideoProgressMapper;
import com.cloudmedia.model.entity.MediaFile;
import com.cloudmedia.model.entity.VideoProgress;
import com.cloudmedia.model.vo.MediaItemVO;
import com.cloudmedia.model.vo.UploadResultVO;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;
import com.cloudmedia.util.FileTypeUtil;
import com.cloudmedia.util.PathUtil;

@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_DOC = "DOC";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DELETED = "DELETED";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MediaFileMapper mediaFileMapper;
    private final VideoProgressMapper videoProgressMapper;
    private final StorageProperties storageProperties;
    private final GuestProperties guestProperties;
    private final long maxUploadSizeBytes;

    public MediaService(MediaFileMapper mediaFileMapper, VideoProgressMapper videoProgressMapper,
                        StorageProperties storageProperties, GuestProperties guestProperties,
                        @Value("${upload.max-file-size}") long maxUploadSizeBytes) {
        this.mediaFileMapper = mediaFileMapper;
        this.videoProgressMapper = videoProgressMapper;
        this.storageProperties = storageProperties;
        this.guestProperties = guestProperties;
        this.maxUploadSizeBytes = maxUploadSizeBytes;
    }

    public UploadResultVO uploadVideo(String guestId, MultipartFile file) {
        validateUploadFile(file);
        String originalName = sanitizeOriginalName(file.getOriginalFilename(), "unknown.mp4");
        String ext = FileTypeUtil.extractExt(originalName);
        String mimeType = normalizedMimeType(file.getContentType());
        if (!FileTypeUtil.isSupportedVideoExt(ext) || !FileTypeUtil.isSupportedVideoMime(mimeType)) {
            throw new ApiException(ApiCode.UNSUPPORTED_FILE_TYPE, "only mp4/m4v with video/* mime is supported");
        }
        return saveUploadedFile(guestId, file, TYPE_VIDEO, ext, originalName, mimeType);
    }

    public UploadResultVO uploadDoc(String guestId, MultipartFile file) {
        validateUploadFile(file);
        String originalName = sanitizeOriginalName(file.getOriginalFilename(), "unknown.pdf");
        String ext = FileTypeUtil.extractExt(originalName);
        String mimeType = normalizedMimeType(file.getContentType());
        if (!FileTypeUtil.isSupportedDocExt(ext) || !FileTypeUtil.isSupportedDocMime(mimeType)) {
            throw new ApiException(ApiCode.UNSUPPORTED_FILE_TYPE, "only pdf/doc/docx is supported");
        }
        return saveUploadedFile(guestId, file, TYPE_DOC, ext, originalName, mimeType);
    }

    public List<MediaItemVO> listByType(String guestId, String type) {
        validateType(type);
        cleanupExpiredByGuest(guestId);
        LocalDateTime now = LocalDateTime.now();
        List<MediaFile> items = mediaFileMapper.selectList(new LambdaQueryWrapper<MediaFile>()
                .eq(MediaFile::getGuestId, guestId)
                .eq(MediaFile::getMediaType, type)
                .eq(MediaFile::getStatus, STATUS_ACTIVE)
                .gt(MediaFile::getExpiresAt, now)
                .orderByDesc(MediaFile::getCreatedAt));
        return items.stream().map(this::toMediaItemVO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnedMedia(String guestId, String id) {
        deleteByEntity(getOwnedMediaForRead(guestId, id));
    }

    public MediaFile getOwnedMediaForRead(String guestId, String id) {
        MediaFile mediaFile = mediaFileMapper.selectById(id);
        if (mediaFile == null || !STATUS_ACTIVE.equals(mediaFile.getStatus())) {
            throw new ApiException(ApiCode.NOT_FOUND, "media not found");
        }
        if (!guestId.equals(mediaFile.getGuestId())) {
            throw new ApiException(ApiCode.FORBIDDEN, "no permission");
        }
        if (isExpired(mediaFile)) {
            deleteByEntity(mediaFile);
            throw new ApiException(ApiCode.GONE, "media expired");
        }
        return mediaFile;
    }

    public MediaFile getOwnedVideoForProgress(String guestId, String id) {
        MediaFile mediaFile = getOwnedMediaForRead(guestId, id);
        if (!TYPE_VIDEO.equals(mediaFile.getMediaType())) {
            throw new ApiException(ApiCode.BAD_REQUEST, "media is not video");
        }
        return mediaFile;
    }

    public Path resolveStoredPath(MediaFile mediaFile) {
        Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
        return PathUtil.safeResolve(root, mediaFile.getPath());
    }

    public Path resolveDerivedPdfPath(MediaFile mediaFile) {
        if (!StringUtils.hasText(mediaFile.getDerivedPdfRelPath())) {
            return null;
        }
        Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
        return PathUtil.safeResolve(root, mediaFile.getDerivedPdfRelPath());
    }

    public void updateDerivedPdfPath(String mediaId, String derivedPdfRelPath) {
        mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                .eq(MediaFile::getId, mediaId)
                .set(MediaFile::getDerivedPdfRelPath, derivedPdfRelPath));
    }

    public void cleanupExpiredMedia() {
        LocalDateTime now = LocalDateTime.now();
        List<MediaFile> expired = mediaFileMapper.selectList(new LambdaQueryWrapper<MediaFile>()
                .eq(MediaFile::getStatus, STATUS_ACTIVE)
                .le(MediaFile::getExpiresAt, now));
        for (MediaFile mediaFile : expired) {
            try {
                deleteByEntity(mediaFile);
            } catch (Exception e) {
                log.warn("cleanup failed for mediaId={}", mediaFile.getId(), e);
            }
        }
    }

    public Map<String, String> getStorageDirectories() {
        Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
        return Map.of(
                "root", root.toString(),
                "videos", root.resolve("videos").toString(),
                "docs", root.resolve("docs").toString(),
                "tmp", root.resolve("tmp").toString());
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ApiCode.BAD_REQUEST, "file is empty");
        }
        if (file.getSize() <= 0) {
            throw new ApiException(ApiCode.BAD_REQUEST, "file size invalid");
        }
        if (file.getSize() > maxUploadSizeBytes) {
            throw new ApiException(ApiCode.BAD_REQUEST, "file too large");
        }
    }

    private void validateType(String type) {
        if (!TYPE_VIDEO.equals(type) && !TYPE_DOC.equals(type)) {
            throw new ApiException(ApiCode.BAD_REQUEST, "invalid type");
        }
    }

    private UploadResultVO saveUploadedFile(String guestId, MultipartFile file, String mediaType,
                                            String ext, String originalName, String mimeType) {
        cleanupExpiredByGuest(guestId);
        enforceQuota(guestId, file.getSize());

        String mediaId = UUID.randomUUID().toString();
        String storedName = UUID.randomUUID() + "." + ext;
        String relPath = (TYPE_VIDEO.equals(mediaType) ? "videos/" : "docs/") + storedName;
        Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
        Path targetPath = PathUtil.safeResolve(root, relPath);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(ApiCode.INTERNAL_ERROR, "failed to save file");
        }

        LocalDateTime now = LocalDateTime.now();
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(mediaId);
        mediaFile.setGuestId(guestId);
        mediaFile.setMediaType(mediaType);
        mediaFile.setOriginalName(originalName);
        mediaFile.setStoredName(storedName);
        mediaFile.setExt(ext);
        mediaFile.setSizeBytes(file.getSize());
        mediaFile.setMimeType(mimeType);
        mediaFile.setPath(relPath);
        mediaFile.setCreatedAt(now);
        mediaFile.setExpiresAt(now.plusMinutes(Math.max(guestProperties.getTtlMinutes(), 1)));
        mediaFile.setStatus(STATUS_ACTIVE);
        mediaFileMapper.insert(mediaFile);
        UploadResultVO result = new UploadResultVO();
        result.setId(mediaId);
        result.setType(mediaType);
        result.setOriginalName(originalName);
        result.setExpiresAt(mediaFile.getExpiresAt() == null ? "" : mediaFile.getExpiresAt().format(TIME_FORMATTER));
        return result;
    }

    private void enforceQuota(String guestId, long incomingSize) {
        LocalDateTime now = LocalDateTime.now();
        List<MediaFile> activeFiles = mediaFileMapper.selectList(new LambdaQueryWrapper<MediaFile>()
                .eq(MediaFile::getGuestId, guestId)
                .eq(MediaFile::getStatus, STATUS_ACTIVE)
                .gt(MediaFile::getExpiresAt, now));
        if (activeFiles.size() >= Math.max(guestProperties.getMaxFiles(), 1)) {
            throw new ApiException(ApiCode.TOO_MANY_REQUESTS, "guest file count quota exceeded");
        }
        long usedBytes = activeFiles.stream().map(MediaFile::getSizeBytes).reduce(0L, Long::sum);
        if (usedBytes + incomingSize > Math.max(guestProperties.getMaxTotalSizeBytes(), 1L)) {
            throw new ApiException(ApiCode.TOO_MANY_REQUESTS, "guest storage quota exceeded");
        }
    }

    private void cleanupExpiredByGuest(String guestId) {
        LocalDateTime now = LocalDateTime.now();
        List<MediaFile> expired = mediaFileMapper.selectList(new LambdaQueryWrapper<MediaFile>()
                .eq(MediaFile::getGuestId, guestId)
                .eq(MediaFile::getStatus, STATUS_ACTIVE)
                .le(MediaFile::getExpiresAt, now));
        for (MediaFile mediaFile : expired) {
            try {
                deleteByEntity(mediaFile);
            } catch (Exception e) {
                log.warn("inline cleanup failed for mediaId={}", mediaFile.getId(), e);
            }
        }
    }

    private boolean isExpired(MediaFile mediaFile) {
        return mediaFile.getExpiresAt() != null && !LocalDateTime.now().isBefore(mediaFile.getExpiresAt());
    }

    private MediaItemVO toMediaItemVO(MediaFile mediaFile) {
        MediaItemVO vo = new MediaItemVO();
        vo.setId(mediaFile.getId());
        vo.setMediaType(mediaFile.getMediaType());
        vo.setOriginalName(mediaFile.getOriginalName());
        vo.setSizeBytes(mediaFile.getSizeBytes());
        vo.setCreatedAt(mediaFile.getCreatedAt() == null ? "" : mediaFile.getCreatedAt().format(TIME_FORMATTER));
        vo.setExpiresAt(mediaFile.getExpiresAt() == null ? "" : mediaFile.getExpiresAt().format(TIME_FORMATTER));
        if (TYPE_VIDEO.equals(mediaFile.getMediaType())) {
            vo.setPlayUrl("/api/media/stream/video/" + mediaFile.getId());
        }
        if (TYPE_DOC.equals(mediaFile.getMediaType())) {
            vo.setViewUrl("/api/media/view/doc/" + mediaFile.getId());
        }
        return vo;
    }

    private void deleteByEntity(MediaFile mediaFile) {
        if (mediaFile == null || !STATUS_ACTIVE.equals(mediaFile.getStatus())) {
            return;
        }
        if (TYPE_VIDEO.equals(mediaFile.getMediaType())) {
            videoProgressMapper.delete(new LambdaQueryWrapper<VideoProgress>()
                    .eq(VideoProgress::getVideoFileId, mediaFile.getId()));
        }
        mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                .eq(MediaFile::getId, mediaFile.getId())
                .eq(MediaFile::getStatus, STATUS_ACTIVE)
                .set(MediaFile::getStatus, STATUS_DELETED));

        Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
        deleteIfExists(root, mediaFile.getPath());
        if (StringUtils.hasText(mediaFile.getDerivedPdfRelPath())) {
            deleteIfExists(root, mediaFile.getDerivedPdfRelPath());
        }
    }

    private void deleteIfExists(Path root, String relPath) {
        try {
            Path target = PathUtil.safeResolve(root, relPath);
            Files.deleteIfExists(target);
        } catch (Exception e) {
            log.warn("delete file failed. path={}", relPath, e);
        }
    }

    private String sanitizeOriginalName(String input, String fallback) {
        if (!StringUtils.hasText(input)) {
            return fallback;
        }
        return Path.of(input).getFileName().toString();
    }

    private String normalizedMimeType(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT);
    }
}
