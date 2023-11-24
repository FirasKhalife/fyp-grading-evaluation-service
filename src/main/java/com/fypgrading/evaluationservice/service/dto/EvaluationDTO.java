package com.fypgrading.evaluationservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDTO {

    private String id;

    private Integer reviewerId;

    private Integer teamId;

    private String assessment;

    private List<GradedRubricDTO> gradedRubrics;
}
