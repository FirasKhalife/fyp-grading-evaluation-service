package com.fypgrading.evaluationservice.controller;

import com.fypgrading.evaluationservice.entity.validationGroups.SubmitEvaluationValidationGroup;
import com.fypgrading.evaluationservice.service.EvaluationService;
import com.fypgrading.evaluationservice.service.dto.EvaluationDTO;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping("/")
    public ResponseEntity<List<EvaluationDTO>> getSubmittedEvaluations() {
        List<EvaluationDTO> evaluations = evaluationService.getSubmittedEvaluations();
        return ResponseEntity.ok().body(evaluations);
    }

    @GetMapping("/{assessment}/{reviewerId}/{teamId}")
    public ResponseEntity<EvaluationDTO> getEvaluationByReviewerIdAndTeamIdAndAssessment(
        @PathVariable String assessment, @PathVariable UUID reviewerId, @PathVariable Long teamId
    ) {
        EvaluationDTO evaluation = evaluationService.getEvaluationByReviewerIdAndTeamIdAndAssessment(
            reviewerId, teamId, assessment);
        return ResponseEntity.ok().body(evaluation);
    }

    @GetMapping("/{assessment}/{teamId}")
    public ResponseEntity<List<EvaluationDTO>> getTeamEvaluationsByAssessment(@PathVariable String assessment, @PathVariable Long teamId) {
        List<EvaluationDTO> evaluations = evaluationService.getTeamEvaluationByAssessment(assessment, teamId);
        return ResponseEntity.ok().body(evaluations);
    }

    @PostMapping("/draft")
    public ResponseEntity<EvaluationDTO> draftEvaluation(@RequestBody EvaluationDTO gradingDTO) {
        EvaluationDTO createdEvaluation = evaluationService.draftEvaluation(gradingDTO);
        return ResponseEntity.ok().body(createdEvaluation);
    }

    @PostMapping("/submit")
    public ResponseEntity<EvaluationDTO> submitEvaluation(
            @Validated(SubmitEvaluationValidationGroup.class) @RequestBody EvaluationDTO gradingDTO
    ) {
        EvaluationDTO createdEvaluation = evaluationService.submitEvaluation(gradingDTO);
        return ResponseEntity.ok().body(createdEvaluation);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluationDTO> updateEvaluation(@PathVariable String id, @RequestBody EvaluationDTO gradingDTO) {
        EvaluationDTO updateEvaluation = evaluationService.updateEvaluation(id, gradingDTO);
        return ResponseEntity.ok().body(updateEvaluation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EvaluationDTO> deleteEvaluation(@PathVariable String id) {
        evaluationService.deleteEvaluation(id);
        return ResponseEntity.noContent().build();
    }
}
