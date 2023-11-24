package com.fypgrading.evaluationservice.service.mapper;

import com.fypgrading.evaluationservice.entity.GradedRubric;
import com.fypgrading.evaluationservice.service.dto.GradedRubricDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GradedRubricMapper {

    GradedRubricDTO toDTO(GradedRubric rubricEvaluation);

    GradedRubric toEntity(GradedRubricDTO gradedRubricDTO);

    List<GradedRubricDTO> toDTOList(List<GradedRubric> rubricEvaluations);

    List<GradedRubric> toEntityList(List<GradedRubricDTO> gradedRubricDTOS);
}
