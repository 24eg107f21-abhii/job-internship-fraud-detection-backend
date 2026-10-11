package com.abhinay.frauddetection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.abhinay.frauddetection.entity.Job;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

}