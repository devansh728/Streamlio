package com.streamLio.upload_service.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "encoding_jobs")
@Data
public class EncodingJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String s3Path;          // e.g., "s3://bucket/videos/abc123.mp4"
    private String status;         // PENDING, PROCESSING, COMPLETED, FAILED
    private String errorMessage;   // Null if success
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
