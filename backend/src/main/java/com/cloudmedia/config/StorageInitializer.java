package com.cloudmedia.config;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;

@Component
public class StorageInitializer {

    private final StorageProperties storageProperties;

    public StorageInitializer(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void init() {
        try {
            Path root = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
            Files.createDirectories(root.resolve("videos"));
            Files.createDirectories(root.resolve("docs"));
            Files.createDirectories(root.resolve("tmp"));
        } catch (Exception e) {
            throw new ApiException(ApiCode.INTERNAL_ERROR, "failed to initialize storage directories");
        }
    }
}
