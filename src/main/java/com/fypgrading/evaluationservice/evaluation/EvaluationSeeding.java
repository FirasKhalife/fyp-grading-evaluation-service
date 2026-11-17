package com.fypgrading.evaluationservice.evaluation;

import java.time.Instant;
import java.util.List;

/** The one way to set a seeded evaluation's state directly (status, dates, history) when copying the seed. */
public final class EvaluationSeeding {

    private EvaluationSeeding() {}

    public static void restore(
            Evaluation evaluation,
            EvaluationStatus status,
            String overallComment,
            Instant updatedAt,
            Instant submittedAt,
            List<Evaluation.HistoryEntry> history) {
        evaluation.restore(status, overallComment, updatedAt, submittedAt, history);
    }
}
