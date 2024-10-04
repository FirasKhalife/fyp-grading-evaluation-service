package com.fypgrading.evaluationservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeIdDTO {

    private Long teamId;

    private UUID reviewerId;

    private String assessment;

    private Float grade;
}