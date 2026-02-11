package com.cloudmedia.util;

import java.nio.file.Path;

public final class PathUtil {

    private PathUtil() {
    }

    public static Path safeResolve(Path root, String relativePath) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new ApiException(ApiCode.FORBIDDEN, "invalid path");
        }
        return resolved;
    }
}
