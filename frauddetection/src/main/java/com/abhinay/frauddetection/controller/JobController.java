
package com.abhinay.frauddetection.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhinay.frauddetection.dto.MLPredictionResponse;
import com.abhinay.frauddetection.dto.VerificationResponse;
import com.abhinay.frauddetection.entity.Job;
import com.abhinay.frauddetection.service.JobService;
import com.abhinay.frauddetection.service.MLService;
import com.abhinay.frauddetection.service.VerificationService;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final MLService mlService;
    private final VerificationService verificationService;

    public JobController(
            JobService jobService,
            MLService mlService,
            VerificationService verificationService) {

        this.jobService = jobService;
        this.mlService = mlService;
        this.verificationService = verificationService;
    }

    @PostMapping
    public Job createJob(@RequestBody Job job) {
        return jobService.createJob(job);
    }

    @GetMapping
    public List<Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return jobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/predict")
    public MLPredictionResponse predictJob(
            @RequestBody Map<String, Object> jobData) {

        return mlService.predictJob(jobData);
    }

    @PostMapping("/verify")
    public VerificationResponse verifyJob(
            @RequestBody Map<String, Object> jobData) {

        return verificationService.verifyJob(jobData);
    }
}
