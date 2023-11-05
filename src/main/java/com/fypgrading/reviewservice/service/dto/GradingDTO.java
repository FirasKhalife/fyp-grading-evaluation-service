package com.fypgrading.reviewservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradingDTO {

    private Integer id;

    private Integer reviewerId;

    private Integer teamId;

    private List<RubricGradingDTO> rubricsGrades;
}
