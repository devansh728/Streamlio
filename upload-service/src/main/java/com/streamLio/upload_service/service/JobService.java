package com.streamLio.upload_service.service;

import com.streamLio.upload_service.Model.EncodingJob;
import com.streamLio.upload_service.Repository.EncodingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobService {
    private final EncodingRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;

    // Called by UploadService when upload completes
    public EncodingJob createEncodingJob(String s3Path) {
        EncodingJob job = new EncodingJob();
        job.setS3Path(s3Path);
        job.setStatus("PENDING");
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        EncodingJob savedJob = jobRepository.save(job);

        // Publish job ID to encoding queue
        rabbitTemplate.convertAndSend(
                "encoding.exchange",
                "encoding.jobs",
                savedJob.getId()
        );

        return savedJob;
    }

    // Called by EncodingService to update job status
    public void updateJobStatus(Long jobId, String status, String error) {
        EncodingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        job.setStatus(status);
        job.setErrorMessage(error);
        job.setUpdatedAt(LocalDateTime.now());

        jobRepository.save(job);
    }
}
