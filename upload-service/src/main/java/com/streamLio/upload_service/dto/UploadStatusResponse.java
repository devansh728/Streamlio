package com.streamLio.upload_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadStatusResponse {
    private String uploadId;
    private String status;
    private int chunksUploaded;
    private int totalChunks;
    private List<Integer> missingChunks;
}
