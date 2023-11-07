package com.fypgrading.reviewservice.repository;

import com.fypgrading.reviewservice.entity.Evaluation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EvaluationRepository extends MongoRepository<Evaluation, Integer> {

    Long countByTeamId(Integer teamId);
}
