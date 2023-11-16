package com.fypgrading.reviewservice.repository;

import com.fypgrading.reviewservice.entity.Evaluation;
import com.fypgrading.reviewservice.enums.AssessmentEnum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface EvaluationRepository extends MongoRepository<Evaluation, Long> {

    Long countByTeamId(Integer teamId);

    List<Evaluation> getAllByIsSubmitted(boolean isSubmitted);

    List<Evaluation> getAllByAssessmentAndTeamId(AssessmentEnum assessment, Integer teamId);

    Evaluation getByIdAndIsSubmitted(Long id, boolean b);

    Evaluation getByReviewerIdAndTeamIdAndAssessment(int reviewerId, int teamId, AssessmentEnum assessment);
}
