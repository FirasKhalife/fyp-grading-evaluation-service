package com.fypgrading.evaluationservice.demo;

import com.fypgrading.evaluationservice.common.demo.SeedClock;
import com.fypgrading.evaluationservice.common.demo.SeedIds;
import com.fypgrading.evaluationservice.evaluation.Assessment;
import com.fypgrading.evaluationservice.evaluation.Evaluation;
import com.fypgrading.evaluationservice.evaluation.Evaluation.CriterionScore;
import com.fypgrading.evaluationservice.evaluation.Evaluation.HistoryEntry;
import com.fypgrading.evaluationservice.evaluation.Evaluation.HistoryKind;
import com.fypgrading.evaluationservice.evaluation.EvaluationRepository;
import com.fypgrading.evaluationservice.evaluation.EvaluationSeeding;
import com.fypgrading.evaluationservice.evaluation.EvaluationStatus;
import com.fypgrading.evaluationservice.evaluation.ReviewerRole;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** Each visitor's private copy of the evaluations, from {@code demo/seed.json}. Purged at midnight. */
@Service
public class DemoCopyService {

    private static final Logger log = LoggerFactory.getLogger(DemoCopyService.class);

    private final EvaluationRepository evaluations;
    private final MongoTemplate mongo;
    private final Clock clock;
    private final Seed seed;

    public DemoCopyService(EvaluationRepository evaluations, MongoTemplate mongo, Clock clock, JsonMapper json)
            throws IOException {
        this.evaluations = evaluations;
        this.mongo = mongo;
        this.clock = clock;
        try (InputStream in = new ClassPathResource("demo/seed.json").getInputStream()) {
            this.seed = json.readValue(in, Seed.class);
        }
    }

    /**
     * Deletes the session's copy and inserts a fresh one. MongoDB here has no multi-document transactions, so if an
     * autosave lands in between (its draft collides with a seeded one), it deletes and inserts again (review M4).
     */
    public void replace(UUID session, java.time.Instant createdAt) {
        SeedClock at = SeedClock.startingAt(createdAt);
        for (int attempt = 1; ; attempt++) {
            evaluations.deleteByDemoSession(session.toString());
            try {
                insertCopy(session, at);
                return;
            } catch (org.springframework.dao.DuplicateKeyException raced) {
                if (attempt >= 3) {
                    throw raced;
                }
            }
        }
    }

    private void insertCopy(UUID session, SeedClock at) {
        List<Evaluation> copy =
                seed.evaluations().stream().map(e -> toDocument(session, at, e)).toList();
        evaluations.insert(copy);
    }

    public void delete(UUID session) {
        evaluations.deleteByDemoSession(session.toString());
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Paris")
    public void purgeAll() {
        long deleted = mongo.remove(new Query(), Evaluation.class).getDeletedCount();
        log.info("Midnight purge: deleted {} evaluations across all demo copies", deleted);
    }

    private static Evaluation toDocument(UUID session, SeedClock at, SeedEvaluation e) {
        String[] name = e.reviewerName().split(" ", 2);
        List<CriterionScore> criteria = e.criteria().stream()
                .map(c -> new CriterionScore(
                        SeedIds.of(session, c.criterionKey()).toString(),
                        c.position(),
                        c.name(),
                        c.weight(),
                        c.descriptions(),
                        c.level(),
                        c.comment()))
                .toList();
        List<HistoryEntry> history = e.history().stream()
                .map(h -> new HistoryEntry(
                        at.resolve(h.at()),
                        h.kind(),
                        h.criterionKey() == null
                                ? null
                                : SeedIds.of(session, h.criterionKey()).toString(),
                        null,
                        h.from(),
                        h.to()))
                .toList();
        Evaluation document = new Evaluation(
                SeedIds.of(session, e.key()).toString(),
                session.toString(),
                SeedIds.of(session, e.teamKey()).toString(),
                e.teamName(),
                e.assessment(),
                e.reviewerId(),
                name[0],
                name.length > 1 ? name[1] : "",
                e.reviewerRole(),
                criteria,
                at.resolve(e.createdAt()));
        EvaluationSeeding.restore(
                document,
                e.status(),
                e.overallComment(),
                at.resolve(e.updatedAt()),
                at.resolve(e.submittedAt()),
                history);
        return document;
    }

    record Seed(List<SeedEvaluation> evaluations) {}

    record SeedEvaluation(
            String key,
            String teamKey,
            String teamName,
            Assessment assessment,
            String reviewerId,
            String reviewerName,
            ReviewerRole reviewerRole,
            EvaluationStatus status,
            List<SeedCriterion> criteria,
            String overallComment,
            String createdAt,
            String updatedAt,
            String submittedAt,
            List<SeedHistory> history) {}

    record SeedCriterion(
            String criterionKey,
            int position,
            String name,
            int weight,
            Map<String, String> descriptions,
            Integer level,
            String comment) {}

    record SeedHistory(String at, HistoryKind kind, String criterionKey, Integer from, Integer to) {}
}
