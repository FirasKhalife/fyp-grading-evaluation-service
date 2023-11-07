package com.fypgrading.reviewservice.repository;

import com.fypgrading.reviewservice.entity.Grading;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GradingRepository extends MongoRepository<Grading, Integer> {

    Long countByTeamId(Integer teamId);
}
