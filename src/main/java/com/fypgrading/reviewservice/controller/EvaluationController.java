package com.fypgrading.reviewservice.controller;

import com.fypgrading.reviewservice.service.EvaluationService;
import com.fypgrading.reviewservice.service.dto.EvaluationDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/")
    public ResponseEntity<List<EvaluationDTO>> getEvaluations() {
        List<EvaluationDTO> gradings = evaluationService.getEvaluations();
        return ResponseEntity.ok().body(gradings);
    }

    @PostMapping("/draft")
    public ResponseEntity<EvaluationDTO> draftEvaluation(@RequestBody EvaluationDTO gradingDTO) {
        EvaluationDTO createdEvaluation = evaluationService.draftEvaluation(gradingDTO);
        return ResponseEntity.ok().body(createdEvaluation);
    }

    @PostMapping("/submit")
    public ResponseEntity<EvaluationDTO> submitEvaluation(@RequestBody EvaluationDTO gradingDTO) {
        EvaluationDTO createdEvaluation = evaluationService.submitEvaluation(gradingDTO);
        return ResponseEntity.ok().body(createdEvaluation);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluationDTO> updateEvaluation(@PathVariable Long id, @RequestBody EvaluationDTO gradingDTO) {
        EvaluationDTO updateEvaluation = evaluationService.updateEvaluation(id, gradingDTO);
        return ResponseEntity.ok().body(updateEvaluation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EvaluationDTO> deleteEvaluation(@PathVariable Long id) {
        EvaluationDTO deletedEvaluation = evaluationService.deleteEvaluation(id);
        return ResponseEntity.ok().body(deletedEvaluation);
    }
}
