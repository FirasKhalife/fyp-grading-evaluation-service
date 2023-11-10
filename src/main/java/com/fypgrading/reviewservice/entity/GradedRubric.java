package com.fypgrading.reviewservice.entity;

import jakarta.persistence.Id;
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

    private String name;

    private Integer percentage;

    private Integer grade;
}
