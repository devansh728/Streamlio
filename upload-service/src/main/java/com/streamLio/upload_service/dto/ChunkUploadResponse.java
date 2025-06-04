package com.streamLio.upload_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChunkUploadResponse {
    private String uploadId;
    private int chunkIndex;
    private int chunksUploaded;
    private int totalChunks;
    private boolean isDuplicate;
}
