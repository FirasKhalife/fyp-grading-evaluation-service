package com.fypgrading.reviewservice.service;

import com.fypgrading.reviewservice.entity.Evaluation;
import com.fypgrading.reviewservice.repository.EvaluationRepository;
import com.fypgrading.reviewservice.service.dto.CountDTO;
import com.fypgrading.reviewservice.service.dto.EvaluationDTO;
import com.fypgrading.reviewservice.service.event.EvaluationSubmittedEvent;
import com.fypgrading.reviewservice.service.mapper.EvaluationMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class EvaluationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final EvaluationRepository gradingRepository;
    private final EventDispatcher eventDispatcher;
    private final EvaluationMapper gradingMapper;

    public EvaluationService(EvaluationRepository gradingRepository, EvaluationMapper gradingMapper, EventDispatcher eventDispatcher) {
        this.gradingRepository = gradingRepository;
        this.eventDispatcher = eventDispatcher;
        this.gradingMapper = gradingMapper;
    }

    public List<EvaluationDTO> getEvaluations() {
        List<Evaluation> gradings = gradingRepository.findAll();
        return gradingMapper.toDTOList(gradings);
    }

    public EvaluationDTO draftEvaluation(EvaluationDTO gradingDTO) {
        Evaluation grading = gradingMapper.toEntity(gradingDTO);
        Evaluation createdEvaluation = gradingRepository.save(grading);
        return gradingMapper.toDTO(createdEvaluation);
    }

    public EvaluationDTO submitEvaluation(EvaluationDTO gradingDTO) {
        Evaluation grading = gradingMapper.toEntity(gradingDTO);
        Evaluation createdEvaluation = gradingRepository.save(grading);

        EvaluationSubmittedEvent event = new EvaluationSubmittedEvent();
        event.setReviewerId(createdEvaluation.getReviewerId());
        event.setTeamId(createdEvaluation.getTeamId());
        eventDispatcher.sendEvaluationSubmitted(event);

        return gradingMapper.toDTO(createdEvaluation);
    }

    public EvaluationDTO updateEvaluation(Long id, EvaluationDTO gradingDTO) {
        Evaluation grading = gradingMapper.toEntity(gradingDTO);
        grading.setId(id);
        grading = gradingRepository.save(grading);
        return gradingMapper.toDTO(grading);
    }

    public EvaluationDTO deleteEvaluation(Long id) {
        Evaluation grading = getEvaluationById(id);
        gradingRepository.delete(grading);
        return gradingMapper.toDTO(grading);
    }

    private Evaluation getEvaluationById(Long id) {
        return gradingRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Evaluation not found"));
    }

    public void searchForReviewers(EvaluationSubmittedEvent event) {
        ResponseEntity<CountDTO> reviewersResponse = restTemplate.exchange(
                "http://localhost:8080/api/teams/" + event.getTeamId() + "/reviewers",
                HttpMethod.GET, null, new ParameterizedTypeReference<>() {}
        );

        CountDTO reviewersCount = reviewersResponse.getBody();
        if (reviewersCount == null)
            throw new RuntimeException("Reviewers count for team " + event.getTeamId() + " is null");

        Long gradingCount = gradingRepository.countByTeamId(event.getTeamId());

        if (gradingCount.equals(reviewersCount.getCount())) {
            eventDispatcher.sendNotification(event.getTeamId());
        }
    }
}
