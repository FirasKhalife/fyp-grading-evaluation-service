package com.fypgrading.evaluationservice.evaluation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Every query names the demo session explicitly: Mongo has no automatic tenant filter. */
public interface EvaluationRepository extends MongoRepository<Evaluation, String> {

    Optional<Evaluation> findByDemoSessionAndId(String demoSession, String id);

    Optional<Evaluation> findByDemoSessionAndTeamIdAndAssessmentAndReviewerId(String demoSession, String teamId,
            Assessment assessment, String reviewerId);

    List<Evaluation> findByDemoSessionAndTeamIdAndAssessment(String demoSession, String teamId, Assessment assessment);

    List<Evaluation> findByDemoSessionAndTeamIdAndAssessmentAndStatus(String demoSession, String teamId,
            Assessment assessment, EvaluationStatus status);

    List<Evaluation> findByDemoSession(String demoSession);

    long deleteByDemoSession(String demoSession);
}
