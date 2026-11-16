package com.fypgrading.evaluationservice.evaluation;

import com.fypgrading.evaluationservice.clients.AdminClient;
import com.fypgrading.evaluationservice.clients.AdminClient.Assignment;
import com.fypgrading.evaluationservice.clients.AdminClient.GradeReceipt;
import com.fypgrading.evaluationservice.clients.AdminClient.GradeSubmission;
import com.fypgrading.evaluationservice.clients.RubricClient;
import com.fypgrading.evaluationservice.common.demo.DemoSession;
import com.fypgrading.evaluationservice.common.security.CurrentUser;
import com.fypgrading.evaluationservice.common.web.ApiException;
import com.fypgrading.evaluationservice.evaluation.Evaluation.CriterionScore;
import com.fypgrading.evaluationservice.evaluation.Evaluation.HistoryEntry;
import com.fypgrading.evaluationservice.evaluation.Evaluation.HistoryKind;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.CriterionInput;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.CriterionView;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.DraftBody;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.EvaluationSummary;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.EvaluationView;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.HistoryView;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.PersonView;
import com.fypgrading.evaluationservice.events.ActivityPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * The grading form. The reviewer is always the token's subject, and they must hold the assessment's role on
 * the team (checked with admin-service). Scores are computed here, never taken from the client.
 */
@Service
public class EvaluationService {

    private final EvaluationRepository evaluations;
    private final AdminClient admin;
    private final RubricClient rubrics;
    private final ActivityPublisher activity;
    private final Clock clock;

    public EvaluationService(EvaluationRepository evaluations, AdminClient admin, RubricClient rubrics,
            ActivityPublisher activity, Clock clock) {
        this.evaluations = evaluations;
        this.admin = admin;
        this.rubrics = rubrics;
        this.activity = activity;
        this.clock = clock;
    }

    /** My evaluation, or a blank form built from the current rubric (id null, NOT_STARTED). */
    public EvaluationView mine(String teamId, Assessment assessment) {
        CurrentUser me = CurrentUser.require();
        Assignment assignment = requireAssigned(teamId, assessment, me);
        return findMine(teamId, assessment, me)
                .map(e -> view(e, assignment.dueAt()))
                .orElseGet(() -> blank(assignment));
    }

    /** Autosave: the full form state. Creates the draft on first save. */
    public EvaluationView saveDraft(String teamId, Assessment assessment, DraftBody body) {
        CurrentUser me = CurrentUser.require();
        Assignment assignment = requireAssigned(teamId, assessment, me);
        Evaluation saved = retryOnRace(() -> {
            Evaluation evaluation = findMine(teamId, assessment, me).orElse(null);
            boolean created = evaluation == null;
            if (created) {
                evaluation = start(assignment, me);
            }
            if (evaluation.isSubmitted()) {
                throw ApiException.conflict("evaluation-submitted",
                        "This evaluation was submitted and is locked. It can't be changed.");
            }
            List<LevelChange> changes = apply(evaluation, body);
            Evaluation result = evaluations.save(evaluation);
            announce(result, assignment, me, created, changes);
            return result;
        });
        return view(saved, assignment.dueAt());
    }

    /**
     * Submits and locks. The grade is handed to admin-service first (idempotent on the evaluation id), so a
     * failure leaves an editable draft and a retry is always safe.
     */
    public EvaluationView submit(String teamId, Assessment assessment, DraftBody body) {
        CurrentUser me = CurrentUser.require();
        Assignment assignment = requireAssigned(teamId, assessment, me);
        Evaluation evaluation = findMine(teamId, assessment, me).orElse(null);
        if (evaluation != null && evaluation.isSubmitted()) {
            return view(evaluation, assignment.dueAt());
        }
        if (evaluation == null) {
            evaluation = start(assignment, me);
        }
        if (body != null) {
            apply(evaluation, body);
        }
        List<ApiException.FieldProblem> unscored = evaluation.getCriteria().stream()
                .filter(c -> c.level() == null)
                .map(c -> new ApiException.FieldProblem("criteria[" + c.criterionId() + "].level",
                        "\"" + c.name() + "\" has no level yet"))
                .toList();
        if (!unscored.isEmpty()) {
            evaluations.save(evaluation);
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "criteria-unscored",
                    "Score every criterion before submitting: " + unscored.size() + " left.", unscored);
        }
        // Keep the visitor's last edits even if the hand-off below fails.
        evaluation = evaluations.save(evaluation);
        Instant now = clock.instant();
        GradeReceipt receipt = admin.submitGrade(new GradeSubmission(evaluation.getId(), teamId, assessment,
                me.id().toString(), evaluation.score(), now));
        evaluation.submit(now);
        evaluation.record(new HistoryEntry(now, HistoryKind.SUBMITTED, null, null, null, null));
        Evaluation saved = evaluations.save(evaluation);
        activity.publish(DemoSession.require(), "SUBMITTED", me.id().toString(), displayName(saved),
                "submitted the " + assessment.displayName() + " for Team " + saved.getTeamName() + " ("
                        + receipt.submittedCount() + " of " + receipt.expectedCount() + ").",
                now, teamId, assessment);
        return view(saved, assignment.dueAt());
    }

    /** Admin sees drafts and submitted; a reviewer on the team sees submitted ones once the grade is final. */
    public List<EvaluationView> forTeam(String teamId, Assessment assessment) {
        CurrentUser me = CurrentUser.require();
        String session = DemoSession.require().toString();
        Assignment assignment = admin.assignment(teamId, assessment, me.id());
        List<Evaluation> found;
        if (me.isAdmin()) {
            found = evaluations.findByDemoSessionAndTeamIdAndAssessment(session, teamId, assessment);
        } else if (assignment.assigned() && assignment.teamAssessmentFinal()) {
            found = evaluations.findByDemoSessionAndTeamIdAndAssessmentAndStatus(session, teamId, assessment,
                    EvaluationStatus.SUBMITTED);
        } else {
            throw ApiException.forbidden("forbidden",
                    "Other reviewers' evaluations are visible once the team grade is final.");
        }
        return found.stream()
                .sorted(Comparator.comparing(Evaluation::getReviewerLastName))
                .map(e -> view(e, assignment.dueAt()))
                .toList();
    }

    public List<HistoryView> history(String evaluationId) {
        Evaluation e = evaluations.findByDemoSessionAndId(DemoSession.require().toString(), evaluationId)
                .orElseThrow(() -> ApiException.notFound("The evaluation"));
        PersonView actor = reviewer(e);
        Map<String, String> names = e.getCriteria().stream()
                .collect(Collectors.toMap(CriterionScore::criterionId, CriterionScore::name, (a, b) -> a));
        List<HistoryView> trail = new ArrayList<>(e.getHistory().stream()
                .map(h -> new HistoryView(h.at(), actor, h.kind(), h.criterionId(),
                        h.criterionName() != null ? h.criterionName() : names.get(h.criterionId()), h.from(), h.to()))
                .toList());
        trail.sort(Comparator.comparing(HistoryView::at).reversed());
        return trail;
    }

    /** Internal, for admin-service: one line per started evaluation in this demo copy. */
    public List<EvaluationSummary> summaries() {
        return evaluations.findByDemoSession(DemoSession.require().toString()).stream()
                .map(e -> new EvaluationSummary(e.getId(), e.getTeamId(), e.getAssessment(), e.getReviewerId(),
                        e.getStatus(), e.scoredCount(), e.getCriteria().size(),
                        e.scoredCount() == 0 ? null : e.score(), e.getUpdatedAt(), e.getSubmittedAt()))
                .toList();
    }

    /** Internal, for admin-service (student results): submitted evaluations of one team and assessment. */
    public List<EvaluationView> submittedFor(String teamId, Assessment assessment) {
        return evaluations.findByDemoSessionAndTeamIdAndAssessmentAndStatus(DemoSession.require().toString(),
                        teamId, assessment, EvaluationStatus.SUBMITTED).stream()
                .sorted(Comparator.comparing(Evaluation::getReviewerLastName))
                .map(e -> view(e, null))
                .toList();
    }

    // ------------------------------------------------------------------ internals

    private Assignment requireAssigned(String teamId, Assessment assessment, CurrentUser me) {
        Assignment assignment = admin.assignment(teamId, assessment, me.id());
        if (!assignment.assigned()) {
            throw ApiException.forbidden("not-assigned", "You don't grade the " + assessment.displayName()
                    + " for Team " + assignment.teamName() + ".");
        }
        return assignment;
    }

    private java.util.Optional<Evaluation> findMine(String teamId, Assessment assessment, CurrentUser me) {
        return evaluations.findByDemoSessionAndTeamIdAndAssessmentAndReviewerId(DemoSession.require().toString(),
                teamId, assessment, me.id().toString());
    }

    private Evaluation start(Assignment assignment, CurrentUser me) {
        RubricClient.Rubric rubric = rubrics.rubric(assignment.assessment());
        if (rubric.criteria() == null || rubric.criteria().isEmpty()) {
            throw ApiException.conflict("rubric-empty", "This assessment has no rubric yet.");
        }
        List<CriterionScore> snapshot = rubric.criteria().stream()
                .sorted(Comparator.comparingInt(RubricClient.Criterion::position))
                .map(c -> new CriterionScore(c.id(), c.position(), c.name(), c.weight(), c.descriptions(), null, null))
                .toList();
        Instant now = clock.instant();
        Evaluation evaluation = new Evaluation(UUID.randomUUID().toString(), DemoSession.require().toString(),
                assignment.teamId(), assignment.teamName(), assignment.assessment(), me.id().toString(),
                assignment.reviewer().firstName(), assignment.reviewer().lastName(), assignment.requiredRole(),
                snapshot, now);
        evaluation.record(new HistoryEntry(now, HistoryKind.CREATED, null, null, null, null));
        return evaluation;
    }

    private List<LevelChange> apply(Evaluation evaluation, DraftBody body) {
        Map<String, CriterionInput> input = body.criteria().stream()
                .collect(Collectors.toMap(CriterionInput::criterionId, Function.identity(), (a, b) -> b));
        List<String> unknown = input.keySet().stream()
                .filter(id -> evaluation.getCriteria().stream().noneMatch(c -> c.criterionId().equals(id)))
                .toList();
        if (!unknown.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation-failed", "Unknown criteria in the form.",
                    unknown.stream().map(id -> new ApiException.FieldProblem("criteria[" + id + "]",
                            "is not a criterion of this evaluation")).toList());
        }
        Instant now = clock.instant();
        List<LevelChange> changes = new ArrayList<>();
        List<CriterionScore> updated = new ArrayList<>();
        for (CriterionScore c : evaluation.getCriteria()) {
            CriterionInput in = input.get(c.criterionId());
            if (in == null) {
                updated.add(c);
                continue;
            }
            String comment = blankToNull(in.comment());
            if (!Objects.equals(c.level(), in.level())) {
                evaluation.record(new HistoryEntry(now, HistoryKind.LEVEL_CHANGED, c.criterionId(), c.name(),
                        c.level(), in.level()));
                if (c.level() != null && in.level() != null) {
                    changes.add(new LevelChange(c.name(), c.level(), in.level()));
                }
            }
            if (!Objects.equals(c.comment(), comment)) {
                evaluation.record(new HistoryEntry(now, HistoryKind.COMMENT_CHANGED, c.criterionId(), c.name(),
                        null, null));
            }
            updated.add(c.withLevelAndComment(in.level(), comment));
        }
        evaluation.replaceCriteria(updated);
        String overall = blankToNull(body.overallComment());
        if (!Objects.equals(evaluation.getOverallComment(), overall)) {
            evaluation.record(new HistoryEntry(now, HistoryKind.OVERALL_COMMENT_CHANGED, null, null, null, null));
            evaluation.setOverallComment(overall);
        }
        evaluation.touch(now);
        return changes;
    }

    private void announce(Evaluation e, Assignment assignment, CurrentUser me, boolean created,
            List<LevelChange> changes) {
        UUID session = DemoSession.require();
        String draftOf = " " + e.getAssessment().displayName().toLowerCase() + " draft for Team " + e.getTeamName();
        if (created) {
            boolean overdue = assignment.dueAt() != null && clock.instant().isAfter(assignment.dueAt());
            activity.publish(session, "DRAFT_SAVED", me.id().toString(), displayName(e),
                    "saved a draft of the " + e.getAssessment().displayName() + " for Team " + e.getTeamName()
                            + (overdue ? ", which is overdue." : "."),
                    e.getUpdatedAt(), e.getTeamId(), e.getAssessment());
        }
        for (LevelChange change : changes) {
            activity.publish(session, "LEVEL_CHANGED", me.id().toString(), displayName(e),
                    "changed " + change.criterion() + " " + change.from() + " → " + change.to() + " in the"
                            + draftOf + ".",
                    e.getUpdatedAt(), e.getTeamId(), e.getAssessment());
        }
    }

    private static <T> T retryOnRace(java.util.function.Supplier<T> work) {
        try {
            return work.get();
        } catch (DuplicateKeyException | OptimisticLockingFailureException e) {
            return work.get();
        }
    }

    private EvaluationView blank(Assignment assignment) {
        RubricClient.Rubric rubric = rubrics.rubric(assignment.assessment());
        List<CriterionView> criteria = rubric.criteria().stream()
                .sorted(Comparator.comparingInt(RubricClient.Criterion::position))
                .map(c -> new CriterionView(c.id(), c.position(), c.name(), c.weight(), c.descriptions(), null, null))
                .toList();
        AdminClient.Person r = assignment.reviewer();
        return new EvaluationView(null, assignment.teamId(), assignment.teamName(), assignment.assessment(),
                PersonView.of(r.id(), r.firstName(), r.lastName()), assignment.requiredRole(),
                EvaluationStatus.NOT_STARTED, criteria, null, null, 0, criteria.size(), assignment.dueAt(),
                isOverdue(assignment.dueAt(), null), false, null, null, null);
    }

    private EvaluationView view(Evaluation e, Instant dueAt) {
        List<CriterionView> criteria = e.getCriteria().stream()
                .sorted(Comparator.comparingInt(CriterionScore::position))
                .map(c -> new CriterionView(c.criterionId(), c.position(), c.name(), c.weight(), c.descriptions(),
                        c.level(), c.comment()))
                .toList();
        Double score = e.scoredCount() == 0 ? null : Scoring.display(e.score());
        return new EvaluationView(e.getId(), e.getTeamId(), e.getTeamName(), e.getAssessment(), reviewer(e),
                e.getReviewerRole(), e.getStatus(), criteria, e.getOverallComment(), score, e.scoredCount(),
                criteria.size(), dueAt, isOverdue(dueAt, e.getSubmittedAt()), isLate(dueAt, e.getSubmittedAt()),
                e.getCreatedAt(), e.getUpdatedAt(), e.getSubmittedAt());
    }

    private boolean isOverdue(Instant dueAt, Instant submittedAt) {
        return dueAt != null && submittedAt == null && clock.instant().isAfter(dueAt);
    }

    /** Late submissions are allowed and stay marked: submitted after the due date. */
    private static boolean isLate(Instant dueAt, Instant submittedAt) {
        return dueAt != null && submittedAt != null && submittedAt.isAfter(dueAt);
    }

    private static PersonView reviewer(Evaluation e) {
        return PersonView.of(e.getReviewerId(), e.getReviewerFirstName(), e.getReviewerLastName());
    }

    private static String displayName(Evaluation e) {
        return (e.getReviewerFirstName() + " " + e.getReviewerLastName()).trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    private record LevelChange(String criterion, int from, int to) {
    }
}
