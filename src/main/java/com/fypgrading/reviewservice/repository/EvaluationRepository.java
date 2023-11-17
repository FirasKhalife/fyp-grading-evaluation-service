package com.fypgrading.reviewservice.repository;

import com.fypgrading.reviewservice.entity.Evaluation;
import com.fypgrading.reviewservice.enums.AssessmentEnum;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends MongoRepository<Evaluation, String> {

    Long countByTeamId(Integer teamId);

    List<Evaluation> getAllByIsSubmitted(boolean isSubmitted);

    List<Evaluation> getAllByAssessmentAndTeamId(AssessmentEnum assessment, Integer teamId);

    Evaluation getByIdAndIsSubmitted(String id, boolean b);

    Optional<Evaluation> findByReviewerIdAndTeamIdAndAssessment(int reviewerId, int teamId, AssessmentEnum assessment);
}
