package com.fypgrading.reviewservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RubricEvaluation {

    private String name;

    private Integer percentage;

    private Integer grade;
}
