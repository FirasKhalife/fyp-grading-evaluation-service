package com.fypgrading.reviewservice.service;

import com.fypgrading.reviewservice.entity.Evaluation;
import com.fypgrading.reviewservice.entity.validationGroups.SubmitEvaluationValidationGroup;
import com.fypgrading.reviewservice.enums.AssessmentEnum;
import com.fypgrading.reviewservice.repository.EvaluationRepository;
import com.fypgrading.reviewservice.service.dto.EvaluationDTO;
import com.fypgrading.reviewservice.service.dto.EvaluationSubmissionDTO;
import com.fypgrading.reviewservice.service.dto.NotificationDTO;
import com.fypgrading.reviewservice.service.dto.ReviewerDTO;
import com.fypgrading.reviewservice.service.mapper.EvaluationMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

@Service
public class EvaluationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final EvaluationRepository evaluationRepository;
    private final EventDispatcher eventDispatcher;
    private final EvaluationMapper evaluationMapper;

    public EvaluationService(EvaluationRepository evaluationRepository, EvaluationMapper evaluationMapper, EventDispatcher eventDispatcher) {
        this.evaluationRepository = evaluationRepository;
        this.eventDispatcher = eventDispatcher;
        this.evaluationMapper = evaluationMapper;
    }

    public List<EvaluationDTO> getSubmittedEvaluations() {
        List<Evaluation> gradings = evaluationRepository.getAllByIsSubmitted(true);
        return evaluationMapper.toDTOList(gradings);
    }

    public EvaluationDTO getEvaluationByReviewerIdAndTeamIdAndAssessment(
            Integer reviewerId, Integer teamId, String assessmentStr
    ) {
        AssessmentEnum assessment = AssessmentEnum.valueOf(assessmentStr);
        Evaluation evaluation =
                evaluationRepository.getByReviewerIdAndTeamIdAndAssessment(teamId, reviewerId, assessment);
        return evaluationMapper.toDTO(evaluation);
    }

    public EvaluationDTO getEvaluation(Long id) {
        Evaluation evaluation = evaluationRepository.getByIdAndIsSubmitted(id, false);
        return evaluationMapper.toDTO(evaluation);
    }

    public EvaluationDTO draftEvaluation(EvaluationDTO gradingDTO) {
        Evaluation grading = evaluationMapper.toEntity(gradingDTO);
        Evaluation createdEvaluation = evaluationRepository.save(grading);
        return evaluationMapper.toDTO(createdEvaluation);
    }

    public EvaluationDTO submitEvaluation(
            @Validated(SubmitEvaluationValidationGroup.class) EvaluationDTO gradingDTO
    ) {
        Evaluation grading = evaluationMapper.toEntity(gradingDTO);
        Evaluation createdEvaluation = evaluationRepository.save(grading);

        EvaluationSubmissionDTO evaluationSubmission = new EvaluationSubmissionDTO();
        evaluationSubmission.setReviewerId(createdEvaluation.getReviewerId());
        evaluationSubmission.setTeamId(createdEvaluation.getTeamId());
        evaluationSubmission.setAssessment(createdEvaluation.getAssessment());

        ResponseEntity<Void> response = restTemplate.exchange(
                "http://localhost:8082/api/grades/", HttpMethod.POST,
                new HttpEntity<>(evaluationSubmission), new ParameterizedTypeReference<>() {}
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new InternalError("Unexpected error, please try again later");
        }

        return evaluationMapper.toDTO(createdEvaluation);
    }

    public EvaluationDTO updateEvaluation(Long id, EvaluationDTO gradingDTO) {
        if (getEvaluationById(id).getIsSubmitted()) {
            throw new IllegalStateException("Evaluation is submitted, cannot be modified.");
        }

        if (!Objects.equals(id, gradingDTO.getId())) {
            throw new IllegalStateException("Reached evaluation ID and form ID are not the same!");
        }

        Evaluation grading = evaluationMapper.toEntity(gradingDTO);
        grading.setId(id);
        grading = evaluationRepository.save(grading);
        return evaluationMapper.toDTO(grading);
    }

    public EvaluationDTO deleteEvaluation(Long id) {
        Evaluation grading = getEvaluationById(id);
        evaluationRepository.delete(grading);
        return evaluationMapper.toDTO(grading);
    }

    private Evaluation getEvaluationById(Long id) {
        return evaluationRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Evaluation not found"));
    }

    public void submitAssessmentGrade(EvaluationSubmissionDTO evaluationSubmission) {
        ResponseEntity<NotificationDTO> notificationResponse = restTemplate.exchange(
                "http://localhost:8082/api/grades/" + evaluationSubmission.getAssessment().name()
                        + evaluationSubmission.getReviewerId() + "/" + evaluationSubmission.getTeamId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<>() {}
        );
        NotificationDTO notification;
        if (notificationResponse.getStatusCode() != HttpStatus.OK
            || (notification = notificationResponse.getBody()) == null
            || !notification.getNotifyAdmin()) {
            return;
        }

        evaluationSubmissionDispatcher.sendNotification(evaluationSubmission.getTeamId());

    }

    public List<EvaluationDTO> getTeamEvaluationByAssessment(String assessmentStr, Integer teamId) {
        AssessmentEnum assessment = AssessmentEnum.valueOf(assessmentStr);
        List<Evaluation> evaluations = evaluationRepository.getAllByAssessmentAndTeamId(assessment, teamId);
        return evaluationMapper.toDTOList(evaluations);
    }
}
