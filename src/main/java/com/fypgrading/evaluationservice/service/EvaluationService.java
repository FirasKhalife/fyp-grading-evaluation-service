package com.fypgrading.evaluationservice.service;

import com.fypgrading.evaluationservice.entity.Evaluation;
import com.fypgrading.evaluationservice.repository.EvaluationRepository;
import com.fypgrading.evaluationservice.service.client.AdminClient;
import com.fypgrading.evaluationservice.service.client.RubricClient;
import com.fypgrading.evaluationservice.service.dto.EvaluationDTO;
import com.fypgrading.evaluationservice.service.dto.GradeIdDTO;
import com.fypgrading.evaluationservice.service.dto.GradedRubricDTO;
import com.fypgrading.evaluationservice.service.mapper.EvaluationMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationMapper evaluationMapper;
    private RubricClient rubricClient;
    private AdminClient adminClient;

    public List<EvaluationDTO> getSubmittedEvaluations() {
        List<Evaluation> gradings = evaluationRepository.getAllByIsSubmitted(true);
        return evaluationMapper.toDTOList(gradings);
    }

    public EvaluationDTO getEvaluationByReviewerIdAndTeamIdAndAssessment(UUID reviewerId, Long teamId, String assessment) {
        return evaluationRepository.findByReviewerIdAndTeamIdAndAssessment(reviewerId, teamId, assessment.toUpperCase())
            .map(evaluationMapper::toDTO)
            .orElse(
                new EvaluationDTO(reviewerId, teamId, assessment,
                    rubricClient.getRubrics().stream().map(rubric -> new GradedRubricDTO(rubric.getName(), rubric.getPercentage(), 0)).toList()));
    }

    public EvaluationDTO getEvaluation(String id) {
        Evaluation evaluation = evaluationRepository.getByIdAndIsSubmitted(id, false);
        return evaluationMapper.toDTO(evaluation);
    }

    public EvaluationDTO draftEvaluation(EvaluationDTO evaluationDTO) {
        evaluationDTO.setAssessment(evaluationDTO.getAssessment().toUpperCase());
        Evaluation evaluation = evaluationMapper.toEntity(evaluationDTO);
        Evaluation createdEvaluation = evaluationRepository.save(evaluation);
        return evaluationMapper.toDTO(createdEvaluation);
    }

    public EvaluationDTO submitEvaluation(EvaluationDTO evaluationDTO) {
        evaluationDTO.setAssessment(evaluationDTO.getAssessment().toUpperCase());

        if (evaluationDTO.getId() != null)
            evaluationRepository.findById(evaluationDTO.getId())
                .filter(Evaluation::getIsSubmitted)
                .map(evaluation -> { throw new IllegalStateException("Evaluation is already submitted"); });
        else
            evaluationRepository.findByReviewerIdAndTeamIdAndAssessment(
                evaluationDTO.getReviewerId(), evaluationDTO.getTeamId(), evaluationDTO.getAssessment()
            ).map(evaluation -> { throw new IllegalStateException("An evaluation is already drafted for this reviewer and team assessment"); });

        Evaluation evaluation = evaluationMapper.toEntity(evaluationDTO);
        evaluation.setIsSubmitted(true);
        Evaluation createdEvaluation = evaluationRepository.save(evaluation);

        GradeIdDTO gradeIdDTO = new GradeIdDTO();
        gradeIdDTO.setReviewerId(createdEvaluation.getReviewerId());
        gradeIdDTO.setTeamId(createdEvaluation.getTeamId());
        gradeIdDTO.setAssessment(createdEvaluation.getAssessment());

        double finalGrade = evaluation.getGradedRubrics()
                .stream().reduce(0.0, (acc, rubric) ->
                        acc + rubric.getGrade() * rubric.getPercentage(), Double::sum) / 4;
        gradeIdDTO.setGrade((float) finalGrade);

        adminClient.submitGrade(gradeIdDTO);

        return evaluationMapper.toDTO(createdEvaluation);
    }

    public EvaluationDTO updateEvaluation(String id, EvaluationDTO evaluationDTO) {
        if (getEvaluationById(id).getIsSubmitted())
            throw new IllegalStateException("Evaluation is submitted, cannot be modified.");

        evaluationDTO.setAssessment(evaluationDTO.getAssessment().toUpperCase());
        Evaluation evaluation = evaluationMapper.toEntity(evaluationDTO);
        evaluation.setId(id);
        Evaluation createdEvaluation = evaluationRepository.save(evaluation);
        return evaluationMapper.toDTO(createdEvaluation);
    }

    public void deleteEvaluation(String id) {
        Evaluation grading = getEvaluationById(id);
        evaluationRepository.delete(grading);
    }

    private Evaluation getEvaluationById(String id) {
        return evaluationRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Evaluation not found"));
    }

    public List<EvaluationDTO> getTeamEvaluationByAssessment(String assessment, Long teamId) {
        List<Evaluation> evaluations =
            evaluationRepository.getAllByAssessmentAndTeamId(assessment.toUpperCase(), teamId);
        return evaluationMapper.toDTOList(evaluations);
    }
}
