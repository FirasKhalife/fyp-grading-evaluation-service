package com.fypgrading.evaluationservice.entity;

import com.fypgrading.evaluationservice.entity.validationGroups.SubmitEvaluationValidationGroup;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradedRubric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private Integer percentage;

    @NotNull(groups = SubmitEvaluationValidationGroup.class)
    private Integer grade;
}
