package com.fypgrading.evaluationservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDTOList {

    private List<EvaluationDTO> evaluations;

}
