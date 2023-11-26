package com.fypgrading.evaluationservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeIDsDTO {

    private Integer teamId;

    private Integer reviewerId;

    private String assessment;

    private Float grade;
}