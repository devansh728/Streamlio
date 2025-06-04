package com.streamLio.upload_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadCompletionResponse {
    private String uploadId;
    private String filePath;
    private String filename;
    private long fileSize;
    private long completedAt;
}
