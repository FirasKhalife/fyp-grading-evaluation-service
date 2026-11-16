package com.fypgrading.evaluationservice.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.fypgrading.evaluationservice.clients.AdminClient;
import com.fypgrading.evaluationservice.evaluation.Assessment;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos;
import com.fypgrading.evaluationservice.evaluation.EvaluationStatus;
import com.fypgrading.evaluationservice.evaluation.ReviewerRole;
import com.fypgrading.evaluationservice.events.ActivityPublisher;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The payloads evaluation-service exchanges with admin-service and notification-service. */
class PayloadContractsTest {

    static final String ID = "7a1c2f3e-5b6d-4e8f-9a0b-1c2d3e4f5a6b";
    static final Instant AT = Instant.parse("2026-06-10T08:00:00Z");

    @Test
    void producesTheGradeHandOff() {
        Contracts.assertProducesExactly(
                "grade-submission", new AdminClient.GradeSubmission(ID, ID, Assessment.FINAL_REPORT, ID, 72.5, AT));
    }

    @Test
    void producesTheSummariesAndSubmittedEvaluationsAdminServiceReads() {
        Contracts.assertProducesExactly(
                "evaluation-summary",
                new EvaluationDtos.EvaluationSummary(
                        ID, ID, Assessment.FINAL_REPORT, ID, EvaluationStatus.DRAFT, 8, 13, 53.75, AT, AT));
        Contracts.assertProducesExactly(
                "submitted-evaluation",
                new EvaluationDtos.EvaluationView(
                        ID,
                        ID,
                        "Alpha",
                        Assessment.FINAL_PRESENTATION,
                        EvaluationDtos.PersonView.of(ID, "Jane", "Smith"),
                        ReviewerRole.JURY,
                        EvaluationStatus.SUBMITTED,
                        List.of(new EvaluationDtos.CriterionView(
                                ID,
                                1,
                                "Objectives",
                                10,
                                Map.of("4", "All.", "3", "Main.", "2", "Some.", "1", "None."),
                                4,
                                "Clear.")),
                        "A confident defense.",
                        87.5,
                        10,
                        10,
                        AT,
                        false,
                        false,
                        AT,
                        AT,
                        AT));
    }

    @Test
    void producesTheActivityEventNotificationServiceReads() {
        Contracts.assertProducesExactly(
                "activity-event",
                new ActivityPublisher.ActivityEvent(
                        ID,
                        ID,
                        "SUBMITTED",
                        ID,
                        "Jane Smith",
                        "submitted",
                        AT,
                        new ActivityPublisher.Link("TEAM", ID, Assessment.FINAL_REPORT)));
    }

    @Test
    void readsWhatAdminServiceSends() {
        AdminClient.Assignment assignment = Contracts.read("assignment-check", AdminClient.Assignment.class);
        assertThat(assignment.assigned()).isTrue();
        assertThat(assignment.requiredRole()).isEqualTo(ReviewerRole.JURY);
        assertThat(assignment.reviewer().lastName()).isEqualTo("Smith");
        assertThat(assignment.dueAt()).isNotNull();

        AdminClient.GradeReceipt receipt = Contracts.read("grade-receipt", AdminClient.GradeReceipt.class);
        assertThat(receipt.expectedCount()).isEqualTo(3);
    }
}
