package com.fypgrading.reviewservice.repository;

import com.fypgrading.reviewservice.entity.Evaluation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface EvaluationRepository extends MongoRepository<Evaluation, Long> {

    Long countByTeamId(Integer teamId);
}
