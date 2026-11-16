package com.fypgrading.evaluationservice.clients;

import com.fypgrading.evaluationservice.evaluation.Assessment;
import com.fypgrading.evaluationservice.evaluation.ReviewerRole;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** admin-service owns teams, assignments, due dates and the aggregated grades. */
@Component
public class AdminClient {

    private final RestClient http;

    public AdminClient(@Qualifier(ClientConfig.LOAD_BALANCED) RestClient.Builder loadBalanced, @Value("${assay.clients.admin:http://admin-service}") String base) {
        this.http = loadBalanced.baseUrl(base).build();
    }

    /** Whether the reviewer holds this assessment's role on the team, plus what the form needs to show. */
    public Assignment assignment(String teamId, Assessment assessment, UUID reviewerId) {
        return Remote.call("admin service", () -> http.get()
                .uri("/internal/assignments/{team}/{assessment}?reviewerId={reviewer}", teamId, assessment, reviewerId)
                .retrieve()
                .body(Assignment.class));
    }

    /** Stores the reviewer's grade. Idempotent on the evaluation id, so a retried submit is safe. */
    public GradeReceipt submitGrade(GradeSubmission grade) {
        return Remote.call("admin service", () -> http.post()
                .uri("/internal/grades")
                .body(grade)
                .retrieve()
                .body(GradeReceipt.class));
    }

    public record Person(String id, String firstName, String lastName) {
    }

    /**
     * {@code assigned}: the reviewer holds {@code requiredRole} on the team. {@code teamAssessmentFinal}:
     * every expected reviewer has submitted.
     */
    public record Assignment(String teamId, String teamName, Assessment assessment, ReviewerRole requiredRole,
                             boolean assigned, Person reviewer, Instant dueAt, boolean teamAssessmentFinal,
                             int submittedCount, int expectedCount) {
    }

    public record GradeSubmission(String evaluationId, String teamId, Assessment assessment, String reviewerId,
                                  double score, Instant submittedAt) {
    }

    public record GradeReceipt(int submittedCount, int expectedCount) {
    }
}
