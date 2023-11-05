package com.fypgrading.reviewservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RubricGradingDTO {

    private String name;

    private Integer percentage;

    private Integer grade;
}
