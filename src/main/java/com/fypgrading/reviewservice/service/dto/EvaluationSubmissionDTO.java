package com.fypgrading.reviewservice.service.dto;

import com.fypgrading.reviewservice.enums.AssessmentEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationSubmissionDTO {

    private Integer teamId;

    private Integer reviewerId;

    private AssessmentEnum assessment;

    private Float grade;
}