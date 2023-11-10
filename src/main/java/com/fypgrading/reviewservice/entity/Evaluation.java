package com.fypgrading.reviewservice.entity;

import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "evaluation")
public class Evaluation {

    @Id
    private Long id;

    @NotNull
    private Integer reviewerId;

    @NotNull
    private Integer teamId;

    @NotNull
    private List<GradedRubric> gradedRubrics;
}
