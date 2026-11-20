package com.fypgrading.evaluationservice.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fypgrading.evaluationservice.evaluation.EvaluationStatus;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every submitted evaluation in the demo seed has a level on every criterion, as a live submit guarantees. The
 * student's released results (admin-service) rely on it: they never show a made-up level for a missing one.
 */
class SeedInvariantTest {

    @Test
    void submittedSeedEvaluationsHaveEveryLevel() throws Exception {
        DemoCopyService.Seed seed;
        try (InputStream in = new ClassPathResource("demo/seed.json").getInputStream()) {
            seed = JsonMapper.builder().build().readValue(in, DemoCopyService.Seed.class);
        }
        List<String> unscored = seed.evaluations().stream()
                .filter(e -> e.status() == EvaluationStatus.SUBMITTED)
                .flatMap(e ->
                        e.criteria().stream().filter(c -> c.level() == null).map(c -> e.key() + "/" + c.criterionKey()))
                .toList();
        assertThat(seed.evaluations()).isNotEmpty();
        assertThat(unscored).isEmpty();
    }
}
