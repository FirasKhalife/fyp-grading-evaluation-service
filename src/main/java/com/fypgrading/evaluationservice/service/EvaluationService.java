package com.fypgrading.evaluationservice.service;

import com.fypgrading.evaluationservice.entity.Evaluation;
import com.fypgrading.evaluationservice.repository.EvaluationRepository;
import com.fypgrading.evaluationservice.service.client.AdminClient;
import com.fypgrading.evaluationservice.service.client.RubricClient;
import com.fypgrading.evaluationservice.service.dto.EvaluationDTO;
import com.fypgrading.evaluationservice.service.dto.GradeIDsDTO;
import com.fypgrading.evaluationservice.service.dto.GradedRubricDTO;
import com.fypgrading.evaluationservice.service.dto.RubricDTO;
import com.fypgrading.evaluationservice.service.mapper.EvaluationMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor
@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationMapper evaluationMapper;
    private final RubricClient rubricClient;
    private final AdminClient adminClient;

    public List<EvaluationDTO> getSubmittedEvaluations() {
        List<Evaluation> gradings = evaluationRepository.getAllByIsSubmitted(true);
        return evaluationMapper.toDTOList(gradings);
    }

    public EvaluationDTO getEvaluationByReviewerIdAndTeamIdAndAssessment(
        Long reviewerId, Long teamId, String assessment
    ) {
        Optional<Evaluation> evaluation = evaluationRepository.findByReviewerIdAndTeamIdAndAssessment(
                        reviewerId, teamId, assessment.toUpperCase()
        );

        if (evaluation.isPresent())
            return evaluationMapper.toDTO(evaluation.get());

        List<RubricDTO> assessmentRubrics = rubricClient.getRubrics();

        return new EvaluationDTO(
                reviewerId, teamId, assessment,
            assessmentRubrics.stream().map(rubric -> new GradedRubricDTO(rubric.getName(), rubric.getPercentage(), 0)).toList()
        );
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
        if (evaluationDTO.getId() != null) {
            Optional<Evaluation> evaluationOpt = evaluationRepository.findById(evaluationDTO.getId());
            if (evaluationOpt.isPresent() && evaluationOpt.get().getIsSubmitted())
                throw new IllegalStateException("Evaluation is already submitted");
        }

        Evaluation evaluation = evaluationMapper.toEntity(evaluationDTO);
        evaluation.setIsSubmitted(true);
        Evaluation createdEvaluation = evaluationRepository.save(evaluation);

        GradeIDsDTO gradeIDsDTO = new GradeIDsDTO();
        gradeIDsDTO.setReviewerId(createdEvaluation.getReviewerId());
        gradeIDsDTO.setTeamId(createdEvaluation.getTeamId());
        gradeIDsDTO.setAssessment(createdEvaluation.getAssessment());

        double finalGrade = evaluation.getGradedRubrics()
                .parallelStream().reduce(0.0, (acc, rubric) ->
                        acc + rubric.getGrade() * rubric.getPercentage(), Double::sum) / 4;
        gradeIDsDTO.setGrade((float) finalGrade);

        adminClient.submitGrade(gradeIDsDTO);

        return evaluationMapper.toDTO(createdEvaluation);
    }

    public EvaluationDTO updateEvaluation(String id, EvaluationDTO evaluationDTO) {
        if (getEvaluationById(id).getIsSubmitted()) {
            throw new IllegalStateException("Evaluation is submitted, cannot be modified.");
        }

        if (!Objects.equals(id, evaluationDTO.getId())) {
            throw new IllegalStateException("Reached evaluation ID and form ID are not the same!");
        }

        evaluationDTO.setAssessment(evaluationDTO.getAssessment().toUpperCase());
        Evaluation grading = evaluationMapper.toEntity(evaluationDTO);
        grading.setId(id);
        grading = evaluationRepository.save(grading);
        return evaluationMapper.toDTO(grading);
    }

    public EvaluationDTO deleteEvaluation(String id) {
        Evaluation grading = getEvaluationById(id);
        evaluationRepository.delete(grading);
        return evaluationMapper.toDTO(grading);
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
