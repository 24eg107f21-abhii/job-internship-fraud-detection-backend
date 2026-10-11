package com.abhinay.frauddetection.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.abhinay.frauddetection.dto.MLPredictionResponse;

@Service
public class MLService {

    private final RestTemplate restTemplate;

    @Value("${ml.api.url:http://127.0.0.1:8000/predict}")
    private String mlApiUrl;

    public MLService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MLPredictionResponse predictJob(Map<String, Object> jobData) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(jobData, headers);

        ResponseEntity<MLPredictionResponse> response;
        try {
            response = restTemplate.exchange(
                    mlApiUrl,
                    HttpMethod.POST,
                    request,
                    MLPredictionResponse.class
            );
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The machine-learning service is unavailable.",
                    exception
            );
        }

        return response.getBody();
    }
}