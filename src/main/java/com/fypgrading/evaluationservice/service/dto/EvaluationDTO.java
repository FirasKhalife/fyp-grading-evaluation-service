package com.fypgrading.evaluationservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDTO {

    private String id;

    private UUID reviewerId;

    private Long teamId;

    private String assessment;

    private List<GradedRubricDTO> gradedRubrics;

    public EvaluationDTO(UUID reviewerId, Long teamId, String assessment, List<GradedRubricDTO> gradedRubrics) {
        this.reviewerId = reviewerId;
        this.teamId = teamId;
        this.assessment = assessment;
        this.gradedRubrics = gradedRubrics;
    }
}
