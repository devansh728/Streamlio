package com.streamLio.upload_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadInitiationResponse {
    private String uploadId;
    private long chunkSize;
    private int totalChunks;
    private long expiresAt;
}
