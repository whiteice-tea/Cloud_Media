package com.cloudmedia.util;

import java.util.Locale;
import java.util.Set;

public final class FileTypeUtil {

    private static final Set<String> VIDEO_EXTS = Set.of("mp4", "webm", "ogv", "m4v", "mov");
    private static final Set<String> DOC_EXTS = Set.of("pdf", "doc", "docx");
    private static final Set<String> VIDEO_MIMES = Set.of(
            "video/mp4",
            "video/webm",
            "video/ogg",
            "application/ogg",
            "video/quicktime",
            "application/octet-stream"
    );

    private FileTypeUtil() {
    }

    public static String extractExt(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isSupportedVideoExt(String ext) {
        return VIDEO_EXTS.contains(ext.toLowerCase(Locale.ROOT));
    }

    public static boolean isSupportedDocExt(String ext) {
        return DOC_EXTS.contains(ext.toLowerCase(Locale.ROOT));
    }

    public static boolean isSupportedVideoMime(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return true;
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        return VIDEO_MIMES.contains(normalized);
    }

    public static String guessMimeByExt(String ext) {
        String normalized = ext.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "mp4" -> "video/mp4";
            case "m4v" -> "video/mp4";
            case "webm" -> "video/webm";
            case "ogv" -> "video/ogg";
            case "mov" -> "video/quicktime";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }
}
