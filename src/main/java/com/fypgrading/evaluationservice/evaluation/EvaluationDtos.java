package com.fypgrading.evaluationservice.evaluation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Bodies of the evaluation API (contract: Evaluation, EvaluationDraft, EvaluationHistoryEntry). */
public final class EvaluationDtos {

    private EvaluationDtos() {}

    public record PersonView(String id, String firstName, String lastName, String displayName, String initials) {

        public static PersonView of(String id, String firstName, String lastName) {
            String first = firstName == null ? "" : firstName;
            String last = lastName == null ? "" : lastName;
            String initials =
                    (first.isEmpty() ? "" : first.substring(0, 1)) + (last.isEmpty() ? "" : last.substring(0, 1));
            return new PersonView(id, first, last, (first + " " + last).trim(), initials.toUpperCase());
        }
    }

    public record CriterionView(
            String criterionId,
            int position,
            String name,
            int weight,
            Map<String, String> descriptions,
            Integer level,
            String comment) {}

    public record EvaluationView(
            String id,
            String teamId,
            String teamName,
            Assessment assessment,
            PersonView reviewer,
            ReviewerRole reviewerRole,
            EvaluationStatus status,
            List<CriterionView> criteria,
            String overallComment,
            Double score,
            int scoredCount,
            int criteriaCount,
            Instant dueAt,
            boolean overdue,
            boolean submittedLate,
            Instant createdAt,
            Instant updatedAt,
            Instant submittedAt) {}

    public record DraftBody(
            @NotNull @Size(max = 30) List<@Valid @NotNull CriterionInput> criteria,
            @Size(max = 4000) String overallComment) {}

    public record CriterionInput(
            @NotNull String criterionId,
            @Min(0) @Max(4) Integer level,
            @Size(max = 2000) String comment) {}

    public record HistoryView(
            Instant at,
            PersonView actor,
            Evaluation.HistoryKind kind,
            String criterionId,
            String criterionName,
            Integer from,
            Integer to) {}

    /** Internal: what admin-service needs for progress chips and the coordinator grid. */
    public record EvaluationSummary(
            String evaluationId,
            String teamId,
            Assessment assessment,
            String reviewerId,
            EvaluationStatus status,
            int scoredCount,
            int criteriaCount,
            Double score,
            Instant updatedAt,
            Instant submittedAt) {}
}
