package com.streamLio.upload_service.service;
import com.streamLio.upload_service.dto.ChunkUploadResponse;
import com.streamLio.upload_service.dto.UploadCompletionResponse;
import com.streamLio.upload_service.dto.UploadInitiationResponse;
import com.streamLio.upload_service.dto.UploadStatusResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VideoUploadService {

    @Value("${upload.temp.dir}")
    private String tempDir;

    @Value("${upload.storage.dir}")
    private String storageDir;

    @Value("${upload.chunk.size}")
    private long chunkSize;

    // In-memory tracking (replace with Redis in production)
    private final Map<String, UploadSession> uploadSessions = new ConcurrentHashMap<>();

    /**
     * Initialize a new upload session
     */
    public UploadInitiationResponse initiateUpload(String filename, long fileSize, String checksum) {
        // Validate inputs
        if (fileSize <= 0) {
            throw new IllegalArgumentException("Invalid file size");
        }

        String uploadId = UUID.randomUUID().toString();
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        // Create session
        UploadSession session = new UploadSession(
                uploadId,
                filename,
                fileSize,
                checksum,
                totalChunks,
                new HashSet<>(),
                UploadStatus.IN_PROGRESS
        );
        uploadSessions.put(uploadId, session);

        // Create temp directory
        Path uploadPath = Paths.get(tempDir, uploadId);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory", e);
        }

        return new UploadInitiationResponse(
                uploadId,
                chunkSize,
                totalChunks,
                System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24h expiry
        );
    }

    /**
     * Process an individual chunk
     */
    public ChunkUploadResponse processChunk(String uploadId, int chunkIndex,
                                            MultipartFile chunk, String contentRange) {
        // Validate session
        UploadSession session = validateSession(uploadId);

        // Validate chunk index
        if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new IllegalArgumentException("Invalid chunk index");
        }

        // Check for duplicate chunk
        if (session.getReceivedChunks().contains(chunkIndex)) {
            return new ChunkUploadResponse(
                    uploadId,
                    chunkIndex,
                    session.getReceivedChunks().size(),
                    session.getTotalChunks(),
                    true // isDuplicate
            );
        }

        // Validate chunk size (except possibly the last chunk)
        if (chunkIndex != session.getTotalChunks() - 1 &&
                chunk.getSize() != chunkSize) {
            throw new IllegalArgumentException("Invalid chunk size");
        }

        // Save chunk to temp storage
        Path chunkPath = Paths.get(tempDir, uploadId, "chunk_" + chunkIndex);
        try {
            chunk.transferTo(chunkPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save chunk", e);
        }

        // Update session
        session.getReceivedChunks().add(chunkIndex);
        uploadSessions.put(uploadId, session);

        return new ChunkUploadResponse(
                uploadId,
                chunkIndex,
                session.getReceivedChunks().size(),
                session.getTotalChunks(),
                false // isDuplicate
        );
    }

    /**
     * Complete the upload and merge chunks
     */
    public UploadCompletionResponse completeUpload(String uploadId, String checksum) throws IOException {
        UploadSession session = validateSession(uploadId);

        // Verify all chunks are received
        if (session.getReceivedChunks().size() != session.getTotalChunks()) {
            throw new IllegalStateException("Not all chunks received");
        }

        // Verify checksum if provided
        if (checksum != null && !checksum.equals(session.getChecksum())) {
            throw new IllegalArgumentException("Checksum mismatch");
        }

        // Merge chunks
        Path mergedFilePath = mergeChunks(uploadId, session.getFilename());

        // Move to permanent storage (S3 in production)
        Path finalPath = moveToStorage(mergedFilePath, session.getFilename());

        // Cleanup
        cleanup(uploadId);

        // Update session
        session.setStatus(UploadStatus.COMPLETED);
        uploadSessions.put(uploadId, session);

        return new UploadCompletionResponse(
                uploadId,
                finalPath.toString(),
                session.getFilename(),
                session.getFileSize(),
                System.currentTimeMillis()
        );
    }

    /**
     * Get upload status
     */
    public UploadStatusResponse getUploadStatus(String uploadId) {
        UploadSession session = validateSession(uploadId);

        return new UploadStatusResponse(
                uploadId,
                session.getStatus().toString(),
                session.getReceivedChunks().size(),
                session.getTotalChunks(),
                getMissingChunks(session)
        );
    }

    // ===== PRIVATE METHODS =====

    private UploadSession validateSession(String uploadId) {
        UploadSession session = uploadSessions.get(uploadId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid upload ID");
        }
        if (session.getStatus() == UploadStatus.COMPLETED) {
            throw new IllegalStateException("Upload already completed");
        }
        return session;
    }

    private Path mergeChunks(String uploadId, String filename) throws IOException {
        Path uploadPath = Paths.get(tempDir, uploadId);
        Path outputPath = Paths.get(tempDir, uploadId, filename);

        try (OutputStream os = new FileOutputStream(outputPath.toFile())) {
            for (int i = 0; i < uploadSessions.get(uploadId).getTotalChunks(); i++) {
                Path chunkPath = uploadPath.resolve("chunk_" + i);
                Files.copy(chunkPath, os);
            }
        }

        return outputPath;
    }

    private Path moveToStorage(Path source, String filename) throws IOException {
        Path target = Paths.get(storageDir, filename);
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private void cleanup(String uploadId) {
        try {
            Path uploadPath = Paths.get(tempDir, uploadId);
            Files.walk(uploadPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.delete(path); }
                        catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Integer> getMissingChunks(UploadSession session) {
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < session.getTotalChunks(); i++) {
            if (!session.getReceivedChunks().contains(i)) {
                missing.add(i);
            }
        }
        return missing;
    }

    // ===== INNER CLASSES =====

    private enum UploadStatus {
        IN_PROGRESS, COMPLETED, FAILED
    }

    @Data
    @AllArgsConstructor
    private static class UploadSession {
        private final String uploadId;
        private final String filename;
        private final long fileSize;
        private final String checksum;
        private final int totalChunks;
        private final Set<Integer> receivedChunks;
        private UploadStatus status;
    }
}


