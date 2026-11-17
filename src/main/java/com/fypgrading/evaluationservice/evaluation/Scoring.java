package com.fypgrading.evaluationservice.evaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Grade maths from the product brief (4.2). A reviewer's score is Σ(level × criterion weight %) ÷ 4, on 0-100.
 * Unscored criteria count as nothing, so a draft shows its score "so far".
 */
public final class Scoring {

    public static final int MAX_LEVEL = 4;

    private Scoring() {}

    public static double score(Collection<Evaluation.CriterionScore> criteria) {
        int sum = 0;
        for (Evaluation.CriterionScore c : criteria) {
            if (c.level() != null) {
                sum += c.level() * c.weight();
            }
        }
        return sum / (double) MAX_LEVEL;
    }

    /** Scores are shown to one decimal place, rounded half up (21.25 → 21.3). */
    public static double display(double score) {
        return BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
