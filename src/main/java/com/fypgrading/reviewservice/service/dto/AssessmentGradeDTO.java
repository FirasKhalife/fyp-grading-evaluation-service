package com.fypgrading.reviewservice.service.dto;

import com.fypgrading.reviewservice.enums.AssessmentEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentGradeDTO {

    private Integer id;

    private AssessmentEnum assessment;

    private Float grade;

    private Integer reviewerId;

    private Integer teamId;
}

