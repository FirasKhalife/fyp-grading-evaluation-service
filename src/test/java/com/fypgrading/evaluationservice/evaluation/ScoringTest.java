package com.fypgrading.evaluationservice.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fypgrading.evaluationservice.evaluation.Evaluation.CriterionScore;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScoringTest {

    private static CriterionScore c(int weight, Integer level) {
        return new CriterionScore("c" + weight, 1, "criterion", weight, Map.of(), level, null);
    }

    @Test
    void briefExampleLevels432OnWeights204040Gives70() {
        assertThat(Scoring.score(List.of(c(20, 4), c(40, 3), c(40, 2)))).isEqualTo(70.0);
    }

    @Test
    void scoresRunFromZeroToOneHundred() {
        assertThat(Scoring.score(List.of(c(60, 0), c(40, 0)))).isZero();
        assertThat(Scoring.score(List.of(c(60, 4), c(40, 4)))).isEqualTo(100.0);
    }

    @Test
    void aDraftCountsOnlyTheScoredCriteria() {
        assertThat(Scoring.score(List.of(c(50, 4), c(50, null)))).isEqualTo(50.0);
    }

    @Test
    void displayRoundsHalfUpToOneDecimal() {
        assertThat(Scoring.display(21.25)).isEqualTo(21.3);
        assertThat(Scoring.display(53.75)).isEqualTo(53.8);
        assertThat(Scoring.display(86.6666)).isEqualTo(86.7);
    }
}
