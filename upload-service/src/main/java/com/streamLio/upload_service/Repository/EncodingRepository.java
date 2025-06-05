package com.streamLio.upload_service.Repository;

import com.streamLio.upload_service.Model.EncodingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EncodingRepository extends JpaRepository<EncodingJob, Long> {

}
