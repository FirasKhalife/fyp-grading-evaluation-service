package com.fypgrading.evaluationservice.service;

import com.fypgrading.evaluationservice.entity.Evaluation;
import com.fypgrading.evaluationservice.exception.BadResponseException;
import com.fypgrading.evaluationservice.exception.ExceptionResponse;
import com.fypgrading.evaluationservice.repository.EvaluationRepository;
import com.fypgrading.evaluationservice.service.dto.EvaluationDTO;
import com.fypgrading.evaluationservice.service.dto.TeamGradeDTO;
import com.fypgrading.evaluationservice.service.mapper.EvaluationMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class EvaluationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final EvaluationRepository evaluationRepository;
    private final EvaluationMapper evaluationMapper;

    public EvaluationService(EvaluationRepository evaluationRepository, EvaluationMapper evaluationMapper) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationMapper = evaluationMapper;
    }

    public List<EvaluationDTO> getSubmittedEvaluations() {
        List<Evaluation> gradings = evaluationRepository.getAllByIsSubmitted(true);
        return evaluationMapper.toDTOList(gradings);
    }

    public EvaluationDTO getEvaluationByReviewerIdAndTeamIdAndAssessment(
            Integer reviewerId, Integer teamId, String assessment
    ) {
        Evaluation evaluation =
                evaluationRepository.findByReviewerIdAndTeamIdAndAssessment(
                        reviewerId, teamId, assessment.toUpperCase()
                ).orElseThrow(() -> new EntityNotFoundException("No evaluation found"));
        return evaluationMapper.toDTO(evaluation);
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

        TeamGradeDTO grade = new TeamGradeDTO();
        grade.setReviewerId(createdEvaluation.getReviewerId());
        grade.setTeamId(createdEvaluation.getTeamId());
        grade.setAssessment(createdEvaluation.getAssessment());

        double finalGrade = evaluation.getGradedRubrics()
                .parallelStream().reduce(0.0, (acc, rubric) ->
                        acc + rubric.getGrade() * rubric.getPercentage(), Double::sum) / 4;
        grade.setGrade((float) finalGrade);

        ResponseEntity<Object> response = restTemplate.exchange(
                "http://localhost:8081/api/grades/", HttpMethod.POST,
                new HttpEntity<>(grade), new ParameterizedTypeReference<>() {}
        );

        System.out.println(response);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new BadResponseException(response.getStatusCode(), (ExceptionResponse) response.getBody());
        }

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

    public List<EvaluationDTO> getTeamEvaluationByAssessment(String assessment, Integer teamId) {
        List<Evaluation> evaluations =
                evaluationRepository.getAllByAssessmentAndTeamId(assessment.toUpperCase(), teamId);
        return evaluationMapper.toDTOList(evaluations);
    }
}
