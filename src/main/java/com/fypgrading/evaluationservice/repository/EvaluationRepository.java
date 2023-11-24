package com.fypgrading.evaluationservice.repository;

import com.fypgrading.evaluationservice.entity.Evaluation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends MongoRepository<Evaluation, String> {

    Long countByTeamId(Integer teamId);

    List<Evaluation> getAllByIsSubmitted(boolean isSubmitted);

    List<Evaluation> getAllByAssessmentAndTeamId(String assessment, Integer teamId);

    Evaluation getByIdAndIsSubmitted(String id, boolean b);

    Optional<Evaluation> findByReviewerIdAndTeamIdAndAssessment(int reviewerId, int teamId, String assessment);
}
