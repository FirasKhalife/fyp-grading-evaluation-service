package com.fypgrading.reviewservice.entity;

import com.fypgrading.reviewservice.entity.validationGroups.SubmitEvaluationValidationGroup;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "graded_rubric")
public class GradedRubric {

    @Id
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private Integer percentage;

    @NotNull(groups = SubmitEvaluationValidationGroup.class)
    private Integer grade;
}
