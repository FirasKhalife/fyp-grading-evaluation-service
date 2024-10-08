package com.fypgrading.evaluationservice.repository;

import com.fypgrading.evaluationservice.entity.Evaluation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationRepository extends MongoRepository<Evaluation, String> {

    Long countByTeamId(Long teamId);

    List<Evaluation> getAllByIsSubmitted(boolean isSubmitted);

    List<Evaluation> getAllByAssessmentAndTeamId(String assessment, Long teamId);

    Evaluation getByIdAndIsSubmitted(String id, boolean b);

    Optional<Evaluation> findByReviewerIdAndTeamIdAndAssessment(UUID reviewerId, Long teamId, String assessment);
}
