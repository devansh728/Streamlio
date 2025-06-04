package com.streamLio.upload_service.controller;
import com.streamLio.upload_service.dto.ChunkUploadResponse;
import com.streamLio.upload_service.dto.UploadCompletionResponse;
import com.streamLio.upload_service.dto.UploadInitiationResponse;
import com.streamLio.upload_service.dto.UploadStatusResponse;
import com.streamLio.upload_service.service.VideoUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class UploadServiceContoller {
    private final VideoUploadService uploadService;

    public UploadServiceContoller(VideoUploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * Step 1: Initialize a new upload session
     */
    @PostMapping("/initiate")
    public ResponseEntity<UploadInitiationResponse> initiateUpload(
            @RequestParam String filename,
            @RequestParam long fileSize,
            @RequestParam String checksum) {

        UploadInitiationResponse response = uploadService.initiateUpload(filename, fileSize, checksum);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2: Upload individual chunk
     */
    @PostMapping("/upload/{uploadId}/{chunkIndex}")
    public ResponseEntity<ChunkUploadResponse> uploadChunk(
            @PathVariable String uploadId,
            @PathVariable int chunkIndex,
            @RequestParam("file") MultipartFile chunk,
            @RequestHeader("Content-Range") String contentRange) {

        ChunkUploadResponse response = uploadService.processChunk(uploadId, chunkIndex, chunk, contentRange);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 3: Complete the upload and merge chunks
     */
    @PostMapping("/complete/{uploadId}")
    public ResponseEntity<UploadCompletionResponse> completeUpload(
            @PathVariable String uploadId,
            @RequestParam String checksum) throws IOException {

        UploadCompletionResponse response = uploadService.completeUpload(uploadId, checksum);
        return ResponseEntity.ok(response);
    }

    /**
     * Check upload progress
     */
    @GetMapping("/status/{uploadId}")
    public ResponseEntity<UploadStatusResponse> getUploadStatus(
            @PathVariable String uploadId) {

        UploadStatusResponse response = uploadService.getUploadStatus(uploadId);
        return ResponseEntity.ok(response);
    }
}
