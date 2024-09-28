package com.fypgrading.evaluationservice.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "evaluation")
public class Evaluation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @NotNull
    private UUID reviewerId;

    @NotNull
    private Long teamId;

    @NotNull
    private String assessment;

    @NotNull
    private Boolean isSubmitted = false;

    @NotNull
    private List<GradedRubric> gradedRubrics;
}
