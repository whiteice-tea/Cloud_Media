package com.cloudmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.cloudmedia.config.StorageProperties;
import com.cloudmedia.mapper.MediaFileMapper;
import com.cloudmedia.mapper.VideoProgressMapper;
import com.cloudmedia.model.entity.MediaFile;
import com.cloudmedia.model.entity.VideoProgress;
import com.cloudmedia.model.vo.MediaItemVO;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;
import com.cloudmedia.util.FileTypeUtil;
import com.cloudmedia.util.PathUtil;

@Service
public class MediaService {

    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_DOC = "DOC";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MediaFileMapper mediaFileMapper;
    private final VideoProgressMapper videoProgressMapper;
    private final StorageProperties storageProperties;
    private final SignedUrlService signedUrlService;

    public MediaService(MediaFileMapper mediaFileMapper, VideoProgressMapper videoProgressMapper,
                        StorageProperties storageProperties, SignedUrlService signedUrlService) {
        this.mediaFileMapper = mediaFileMapper;
        this.videoProgressMapper = videoProgressMapper;
        this.storageProperties = storageProperties;
        this.signedUrlService = signedUrlService;
    }

    public Long uploadVideo(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ApiCode.BAD_REQUEST, "file is empty");
        }
        String originalName = file.getOriginalFilename() == null ? "unknown.mp4" : file.getOriginalFilename();
        String ext = FileTypeUtil.extractExt(originalName);
        String mimeType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!FileTypeUtil.isSupportedVideoExt(ext) || !FileTypeUtil.isSupportedVideoMime(mimeType)) {
            throw new ApiException(ApiCode.UNSUPPORTED_FILE_TYPE, "supported video formats: mp4, m4v, webm, ogv, mov");
        }
        return saveUploadedFile(userId, file, TYPE_VIDEO, ext, originalName);
    }

    public Long uploadDoc(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ApiCode.BAD_REQUEST, "file is empty");
        }
        String originalName = file.getOriginalFilename() == null ? "unknown.pdf" : file.getOriginalFilename();
        String ext = FileTypeUtil.extractExt(originalName);
        String mimeType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        boolean mimeAccepted = mimeType.isBlank()
                || mimeType.contains("pdf")
                || mimeType.contains("msword")
                || mimeType.contains("officedocument.wordprocessingml.document")
                || mimeType.equals("application/octet-stream");
        if (!FileTypeUtil.isSupportedDocExt(ext) || !mimeAccepted) {
            throw new ApiException(ApiCode.UNSUPPORTED_FILE_TYPE, "only pdf/doc/docx is supported");
        }
        return saveUploadedFile(userId, file, TYPE_DOC, ext, originalName);
    }

    public List<MediaItemVO> listByType(Long userId, String type) {
        validateType(type);
        List<MediaFile> items = mediaFileMapper.selectList(new LambdaQueryWrapper<MediaFile>()
                .eq(MediaFile::getUserId, userId)
                .eq(MediaFile::getMediaType, type)
                .orderByDesc(MediaFile::getCreatedAt));
        return items.stream().map(item -> toMediaItemVO(userId, item)).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnedMedia(Long userId, Long id) {
        deleteByEntity(getOwnedMedia(userId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAllByUserId(Long userId) {
        List<MediaFile> files = mediaFileMapper.selectList(new LambdaQueryWrapper<MediaFile>()
                .eq(MediaFile::getUserId, userId));
        for (MediaFile file : files) {
            deleteByEntity(file);
        }
        videoProgressMapper.delete(new LambdaQueryWrapper<VideoProgress>()
                .eq(VideoProgress::getUserId, userId));
    }

    public MediaFile getOwnedMedia(Long userId, Long id) {
        MediaFile mediaFile = getMediaById(id);
        if (!mediaFile.getUserId().equals(userId)) {
            throw new ApiException(ApiCode.FORBIDDEN, "no permission");
        }
        return mediaFile;
    }

    public MediaFile getMediaById(Long id) {
        MediaFile mediaFile = mediaFileMapper.selectById(id);
        if (mediaFile == null) {
            throw new ApiException(ApiCode.NOT_FOUND, "media not found");
        }
        return mediaFile;
    }

    public Path resolveStoredPath(MediaFile mediaFile) {
        Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
        return PathUtil.safeResolve(root, mediaFile.getStorageRelPath());
    }

    public Path resolveDerivedPdfPath(MediaFile mediaFile) {
        if (mediaFile.getDerivedPdfRelPath() == null || mediaFile.getDerivedPdfRelPath().isBlank()) {
            return null;
        }
        Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
        return PathUtil.safeResolve(root, mediaFile.getDerivedPdfRelPath());
    }

    public void updateDerivedPdfPath(Long mediaId, String derivedPdfRelPath) {
        LocalDateTime now = LocalDateTime.now();
        mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                .eq(MediaFile::getId, mediaId)
                .set(MediaFile::getDerivedPdfRelPath, derivedPdfRelPath)
                .set(MediaFile::getUpdatedAt, now));
    }

    public Map<String, String> getStorageDirectories() {
        Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
        return Map.of(
                "root", root.toString(),
                "videos", root.resolve("videos").toString(),
                "docs", root.resolve("docs").toString(),
                "tmp", root.resolve("tmp").toString());
    }

    private void validateType(String type) {
        if (!TYPE_VIDEO.equals(type) && !TYPE_DOC.equals(type)) {
            throw new ApiException(ApiCode.BAD_REQUEST, "invalid type");
        }
    }

    private Long saveUploadedFile(Long userId, MultipartFile file, String mediaType, String ext, String originalName) {
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

        MediaFile mediaFile = new MediaFile();
        mediaFile.setUserId(userId);
        mediaFile.setMediaType(mediaType);
        mediaFile.setOriginalName(originalName);
        mediaFile.setStoredName(storedName);
        mediaFile.setExt(ext);
        mediaFile.setSizeBytes(file.getSize());
        String mimeType = file.getContentType();
        mediaFile.setMimeType((mimeType == null || mimeType.isBlank()) ? FileTypeUtil.guessMimeByExt(ext) : mimeType);
        mediaFile.setStorageRelPath(relPath);
        LocalDateTime now = LocalDateTime.now();
        mediaFile.setCreatedAt(now);
        mediaFile.setUpdatedAt(now);
        mediaFileMapper.insert(mediaFile);
        return mediaFile.getId();
    }

    private MediaItemVO toMediaItemVO(Long userId, MediaFile mediaFile) {
        MediaItemVO vo = new MediaItemVO();
        vo.setId(mediaFile.getId());
        vo.setMediaType(mediaFile.getMediaType());
        vo.setOriginalName(mediaFile.getOriginalName());
        vo.setSizeBytes(mediaFile.getSizeBytes());
        vo.setCreatedAt(mediaFile.getCreatedAt() == null ? "" : mediaFile.getCreatedAt().format(TIME_FORMATTER));
        if (TYPE_VIDEO.equals(mediaFile.getMediaType())) {
            String accessToken = signedUrlService.generateVideoAccessToken(userId, mediaFile.getId());
            vo.setPlayUrl("/api/media/stream/video/" + mediaFile.getId() + "?accessToken=" + accessToken);
        } else {
            vo.setPlayUrl(null);
        }
        if (TYPE_DOC.equals(mediaFile.getMediaType())) {
            String accessToken = signedUrlService.generateDocAccessToken(userId, mediaFile.getId());
            vo.setViewUrl("/api/media/view/doc/" + mediaFile.getId() + "?accessToken=" + accessToken);
        } else {
            vo.setViewUrl(null);
        }
        return vo;
    }

    private void deleteByEntity(MediaFile mediaFile) {
        videoProgressMapper.delete(new LambdaQueryWrapper<VideoProgress>().eq(VideoProgress::getVideoFileId, mediaFile.getId()));
        int deleted = mediaFileMapper.deleteById(mediaFile.getId());
        if (deleted != 1) {
            throw new ApiException(ApiCode.INTERNAL_ERROR, "failed to delete media");
        }
        Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
        deleteIfExists(root, mediaFile.getStorageRelPath());
        if (mediaFile.getDerivedPdfRelPath() != null && !mediaFile.getDerivedPdfRelPath().isBlank()) {
            deleteIfExists(root, mediaFile.getDerivedPdfRelPath());
        }
    }

    private void deleteIfExists(Path root, String relPath) {
        try {
            Path target = PathUtil.safeResolve(root, relPath);
            Files.deleteIfExists(target);
        } catch (Exception ignored) {
        }
    }
}
