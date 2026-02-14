package com.cloudmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.cloudmedia.config.StorageProperties;
import com.cloudmedia.mapper.MediaFileMapper;
import com.cloudmedia.model.entity.MediaFile;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;
import com.cloudmedia.util.PathUtil;

@Service
public class WordConvertService {

    private static final Logger log = LoggerFactory.getLogger(WordConvertService.class);
    private static final ConcurrentHashMap<String, ReentrantLock> DOC_LOCKS = new ConcurrentHashMap<>();

    private final StorageProperties storageProperties;
    private final MediaFileMapper mediaFileMapper;
    private final MediaService mediaService;

    public WordConvertService(StorageProperties storageProperties, MediaFileMapper mediaFileMapper, MediaService mediaService) {
        this.storageProperties = storageProperties;
        this.mediaFileMapper = mediaFileMapper;
        this.mediaService = mediaService;
    }

    public Path ensureDerivedPdf(MediaFile mediaFile) {
        if (!"DOC".equals(mediaFile.getMediaType())) {
            throw new ApiException(ApiCode.BAD_REQUEST, "media is not doc");
        }

        if ("pdf".equalsIgnoreCase(mediaFile.getExt())) {
            return mediaService.resolveStoredPath(mediaFile);
        }

        ReentrantLock lock = DOC_LOCKS.computeIfAbsent(mediaFile.getId(), key -> new ReentrantLock());
        lock.lock();
        try {
            MediaFile latest = mediaFileMapper.selectOne(new LambdaQueryWrapper<MediaFile>()
                    .eq(MediaFile::getId, mediaFile.getId())
                    .last("LIMIT 1"));
            if (latest == null) {
                throw new ApiException(ApiCode.NOT_FOUND, "media not found");
            }
            if (latest.getDerivedPdfRelPath() != null && !latest.getDerivedPdfRelPath().isBlank()) {
                Path ready = mediaService.resolveDerivedPdfPath(latest);
                if (ready != null && Files.exists(ready)) {
                    return ready;
                }
            }
            return convertToPdf(latest);
        } finally {
            lock.unlock();
        }
    }

    private Path convertToPdf(MediaFile mediaFile) {
        Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
        Path sourcePath = mediaService.resolveStoredPath(mediaFile);
        String ext = mediaFile.getExt().toLowerCase();

        if (!Objects.equals(ext, "doc") && !Objects.equals(ext, "docx")) {
            throw new ApiException(ApiCode.BAD_REQUEST, "only doc/docx needs conversion");
        }

        String tmpName = UUID.randomUUID() + "." + ext;
        String outputName = UUID.randomUUID() + ".pdf";
        Path tmpInputPath = PathUtil.safeResolve(root, "tmp/" + tmpName);
        Path tmpPdfPath = PathUtil.safeResolve(root, "tmp/" + tmpName.substring(0, tmpName.lastIndexOf('.')) + ".pdf");
        Path finalPdfPath = PathUtil.safeResolve(root, "docs/" + outputName);

        try {
            Files.copy(sourcePath, tmpInputPath, StandardCopyOption.REPLACE_EXISTING);
            Process process = new ProcessBuilder(
                    "soffice",
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", tmpInputPath.getParent().toString(),
                    tmpInputPath.toString()
            ).redirectErrorStream(true).start();
            int exitCode = process.waitFor();
            if (exitCode != 0 || !Files.exists(tmpPdfPath)) {
                log.error("Word convert failed. exitCode={}, source={}", exitCode, sourcePath);
                throw new ApiException(ApiCode.WORD_CONVERT_FAILED, "word convert failed, check libreoffice installation");
            }

            Files.move(tmpPdfPath, finalPdfPath, StandardCopyOption.REPLACE_EXISTING);
            String rel = "docs/" + outputName;
            mediaService.updateDerivedPdfPath(mediaFile.getId(), rel);
            mediaFile.setDerivedPdfRelPath(rel);
            return finalPdfPath;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Word convert exception for mediaId={}", mediaFile.getId(), e);
            throw new ApiException(ApiCode.WORD_CONVERT_FAILED, "word convert failed");
        } finally {
            cleanup(tmpInputPath);
            cleanup(tmpPdfPath);
        }
    }

    private void cleanup(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
