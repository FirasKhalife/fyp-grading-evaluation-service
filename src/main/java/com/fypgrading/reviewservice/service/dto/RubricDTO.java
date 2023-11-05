package com.fypgrading.reviewservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RubricDTO {

    private String name;

    private Integer percentage;
}