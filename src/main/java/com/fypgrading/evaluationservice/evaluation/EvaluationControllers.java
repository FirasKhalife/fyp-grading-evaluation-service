package com.fypgrading.evaluationservice.evaluation;

import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.DraftBody;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.EvaluationSummary;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.EvaluationView;
import com.fypgrading.evaluationservice.evaluation.EvaluationDtos.HistoryView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry points. Kept thin: the rules live in {@link EvaluationService}. */
public final class EvaluationControllers {

    private EvaluationControllers() {}

    /** P4: the signed-in reviewer's own evaluation. */
    @RestController
    @RequestMapping("/api/my-evaluations/{teamId}/{assessment}")
    @PreAuthorize("hasRole('REVIEWER')")
    public static class Mine {

        private final EvaluationService service;

        public Mine(EvaluationService service) {
            this.service = service;
        }

        @GetMapping
        public EvaluationView get(@PathVariable UUID teamId, @PathVariable Assessment assessment) {
            return service.mine(teamId.toString(), assessment);
        }

        @PutMapping
        public EvaluationView saveDraft(
                @PathVariable UUID teamId, @PathVariable Assessment assessment, @Valid @RequestBody DraftBody body) {
            return service.saveDraft(teamId.toString(), assessment, body);
        }

        @PostMapping("/submit")
        public EvaluationView submit(
                @PathVariable UUID teamId,
                @PathVariable Assessment assessment,
                @Valid @RequestBody(required = false) DraftBody body) {
            return service.submit(teamId.toString(), assessment, body);
        }
    }

    /** P6: everyone's evaluations for one team and assessment, and the draft audit trail. */
    @RestController
    @RequestMapping("/api/evaluations")
    public static class Team {

        private final EvaluationService service;

        public Team(EvaluationService service) {
            this.service = service;
        }

        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
        public List<EvaluationView> list(@RequestParam UUID teamId, @RequestParam Assessment assessment) {
            return service.forTeam(teamId.toString(), assessment);
        }

        @GetMapping("/{evaluationId}/history")
        @PreAuthorize("hasRole('ADMIN')")
        public List<HistoryView> history(@PathVariable UUID evaluationId) {
            return service.history(evaluationId.toString());
        }
    }

    /** Internal, for admin-service only (the gateway doesn't route /internal/**). */
    @RestController
    @RequestMapping("/internal/evaluations")
    public static class Internal {

        private final EvaluationService service;

        public Internal(EvaluationService service) {
            this.service = service;
        }

        @GetMapping("/summary")
        public List<EvaluationSummary> summary() {
            return service.summaries();
        }

        @GetMapping("/submitted")
        public List<EvaluationView> submitted(@RequestParam UUID teamId, @RequestParam Assessment assessment) {
            return service.submittedFor(teamId.toString(), assessment);
        }
    }
}
