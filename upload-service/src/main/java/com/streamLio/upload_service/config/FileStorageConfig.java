package com.streamLio.upload_service.config;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {

    @Value("${upload.temp.dir}")
    private String tempDir;

    @Value("${upload.storage.dir}")
    private String storageDir;

    @PostConstruct
    public void init() throws IOException {
        Path tempPath = Paths.get(tempDir);
        Path storagePath = Paths.get(storageDir);

        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath);
        }

        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }
    }
}
