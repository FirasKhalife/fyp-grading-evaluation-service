package com.fypgrading.evaluationservice.evaluation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One reviewer's filled-in rubric for one team and one assessment. The criteria are a snapshot of the rubric
 * taken when the draft was created, so later rubric edits never change it. Once submitted it's locked.
 */
@Document("evaluations")
@CompoundIndex(
        name = "one_per_reviewer",
        unique = true,
        def = "{'demoSession': 1, 'teamId': 1, 'assessment': 1, 'reviewerId': 1}")
public class Evaluation {

    @Id
    private String id;

    @Version
    private Long version;

    private String demoSession;
    private String teamId;
    private String teamName;
    private Assessment assessment;
    private String reviewerId;
    private String reviewerFirstName;
    private String reviewerLastName;
    private ReviewerRole reviewerRole;
    private EvaluationStatus status;
    private List<CriterionScore> criteria = new ArrayList<>();
    private String overallComment;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant submittedAt;
    /** Submitted after the due date in force at the time; stored, so moving the due date never clears it. */
    private boolean submittedLate;

    private List<HistoryEntry> history = new ArrayList<>();

    protected Evaluation() {}

    public Evaluation(
            String id,
            String demoSession,
            String teamId,
            String teamName,
            Assessment assessment,
            String reviewerId,
            String reviewerFirstName,
            String reviewerLastName,
            ReviewerRole reviewerRole,
            List<CriterionScore> criteria,
            Instant createdAt) {
        this.id = id;
        this.demoSession = demoSession;
        this.teamId = teamId;
        this.teamName = teamName;
        this.assessment = assessment;
        this.reviewerId = reviewerId;
        this.reviewerFirstName = reviewerFirstName;
        this.reviewerLastName = reviewerLastName;
        this.reviewerRole = reviewerRole;
        this.criteria = new ArrayList<>(criteria);
        this.status = EvaluationStatus.DRAFT;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public boolean isSubmitted() {
        return status == EvaluationStatus.SUBMITTED;
    }

    public double score() {
        return Scoring.score(criteria);
    }

    public int scoredCount() {
        return (int) criteria.stream().filter(c -> c.level() != null).count();
    }

    public void replaceCriteria(List<CriterionScore> updated) {
        this.criteria = new ArrayList<>(updated);
    }

    public void setOverallComment(String overallComment) {
        this.overallComment = overallComment;
    }

    public void touch(Instant at) {
        this.updatedAt = at;
    }

    public boolean isDraft() {
        return status == EvaluationStatus.DRAFT;
    }

    public boolean isSubmitting() {
        return status == EvaluationStatus.SUBMITTING;
    }

    /** Freezes the draft (and so its score) before the grade is handed over. */
    public void beginSubmit(Instant at, boolean late) {
        this.status = EvaluationStatus.SUBMITTING;
        this.submittedAt = at;
        this.submittedLate = late;
    }

    /** The hand-off failed: the reviewer can edit again. */
    public void abortSubmit() {
        this.status = EvaluationStatus.DRAFT;
        this.submittedAt = null;
        this.submittedLate = false;
    }

    /** The hand-off succeeded: locked for good, at the instant it was frozen. */
    public void completeSubmit() {
        this.status = EvaluationStatus.SUBMITTED;
        this.updatedAt = submittedAt;
    }

    public boolean isSubmittedLate() {
        return submittedLate;
    }

    /** Only for seeding a visitor's copy. */
    void restore(
            EvaluationStatus status,
            String overallComment,
            Instant updatedAt,
            Instant submittedAt,
            List<HistoryEntry> history) {
        this.submittedLate = false;
        this.status = status;
        this.overallComment = overallComment;
        this.updatedAt = updatedAt;
        this.submittedAt = submittedAt;
        this.history = new ArrayList<>(history);
    }

    public void record(HistoryEntry entry) {
        history.add(entry);
    }

    /** Like {@link #record}, but replaces the last entry when it's the same kind of change to the same criterion. */
    public void recordCollapsing(HistoryEntry entry) {
        if (!history.isEmpty()) {
            HistoryEntry last = history.getLast();
            if (last.kind() == entry.kind() && java.util.Objects.equals(last.criterionId(), entry.criterionId())) {
                history.set(history.size() - 1, entry);
                return;
            }
        }
        history.add(entry);
    }

    public String getId() {
        return id;
    }

    public String getDemoSession() {
        return demoSession;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public String getReviewerFirstName() {
        return reviewerFirstName;
    }

    public String getReviewerLastName() {
        return reviewerLastName;
    }

    public ReviewerRole getReviewerRole() {
        return reviewerRole;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public List<CriterionScore> getCriteria() {
        return List.copyOf(criteria);
    }

    public String getOverallComment() {
        return overallComment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public List<HistoryEntry> getHistory() {
        return List.copyOf(history);
    }

    /** A criterion as it was in the rubric when the draft started, plus the reviewer's level and comment. */
    public record CriterionScore(
            String criterionId,
            int position,
            String name,
            int weight,
            Map<String, String> descriptions,
            Integer level,
            String comment) {

        public CriterionScore withLevelAndComment(Integer newLevel, String newComment) {
            return new CriterionScore(criterionId, position, name, weight, descriptions, newLevel, newComment);
        }
    }

    public enum HistoryKind {
        CREATED,
        LEVEL_CHANGED,
        COMMENT_CHANGED,
        OVERALL_COMMENT_CHANGED,
        SUBMITTED
    }

    /** The draft audit trail ("Jane changed Problem formulation 2 → 3"). */
    public record HistoryEntry(
            Instant at, HistoryKind kind, String criterionId, String criterionName, Integer from, Integer to) {}
}
