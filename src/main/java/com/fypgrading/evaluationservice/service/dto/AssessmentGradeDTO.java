package com.fypgrading.evaluationservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentGradeDTO {

    private Integer id;

    private String assessment;

    private Float grade;

    private Integer reviewerId;

    private Integer teamId;
}

