package com.fypgrading.reviewservice.service;

import com.fypgrading.reviewservice.entity.Grading;
import com.fypgrading.reviewservice.repository.GradingRepository;
import com.fypgrading.reviewservice.service.dto.GradingDTO;
import com.fypgrading.reviewservice.service.dto.ReviewerDTO;
import com.fypgrading.reviewservice.service.event.GradingSubmittedEvent;
import com.fypgrading.reviewservice.service.mapper.GradingMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class GradingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final GradingRepository gradingRepository;
    private final EventDispatcher eventDispatcher;
    private final GradingMapper gradingMapper;

    public GradingService(GradingRepository gradingRepository, GradingMapper gradingMapper, EventDispatcher eventDispatcher) {
        this.gradingRepository = gradingRepository;
        this.eventDispatcher = eventDispatcher;
        this.gradingMapper = gradingMapper;
    }

    public List<GradingDTO> getGradings() {
        List<Grading> gradings = gradingRepository.findAll();
        return gradingMapper.toDTOList(gradings);
    }

    public GradingDTO draftGrading(GradingDTO gradingDTO) {
        Grading grading = gradingMapper.toEntity(gradingDTO);
        Grading createdGrading = gradingRepository.save(grading);
        return gradingMapper.toDTO(createdGrading);
    }

    public GradingDTO submitGrading(GradingDTO gradingDTO) {
        Grading grading = gradingMapper.toEntity(gradingDTO);
        Grading createdGrading = gradingRepository.save(grading);

        GradingSubmittedEvent event = new GradingSubmittedEvent();
        event.setReviewerId(createdGrading.getReviewerId());
        event.setTeamId(createdGrading.getTeamId());
        eventDispatcher.sendGradingSubmitted(event);

        return gradingMapper.toDTO(createdGrading);
    }

    public GradingDTO updateGrading(Integer id, GradingDTO gradingDTO) {
        Grading grading = gradingMapper.toEntity(gradingDTO);
        grading.setId(id);
        grading = gradingRepository.save(grading);
        return gradingMapper.toDTO(grading);
    }

    public GradingDTO deleteGrading(Integer id) {
        Grading grading = getGradingById(id);
        gradingRepository.delete(grading);
        return gradingMapper.toDTO(grading);
    }

    private Grading getGradingById(Integer id) {
        return gradingRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Grading not found"));
    }

    public void searchForReviewers(GradingSubmittedEvent event) {
        ResponseEntity<List<ReviewerDTO>> reviewersResponse = restTemplate.exchange(
                "http://localhost:8080/api/teams/" + event.getTeamId() + "/reviewers",
                HttpMethod.GET, null, new ParameterizedTypeReference<>() {}
        );

        List<ReviewerDTO> reviewerList = reviewersResponse.getBody();

        assert reviewerList != null;
        List<Grading> gradingList =
                gradingRepository.findByTeamIdAndReviewerIdIn(
                        event.getTeamId(),
                        reviewerList.parallelStream().map(ReviewerDTO::getId).toList()
                );

        if (gradingList.size() == reviewerList.size()) {
            eventDispatcher.sendNotification(event.getTeamId());
        }
    }
}
