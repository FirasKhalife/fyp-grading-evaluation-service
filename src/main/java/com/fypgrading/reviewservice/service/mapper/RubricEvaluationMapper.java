package com.fypgrading.reviewservice.service.mapper;

import com.fypgrading.reviewservice.entity.GradedRubric;
import com.fypgrading.reviewservice.service.dto.GradedRubricDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RubricEvaluationMapper {

    GradedRubricDTO toDTO(GradedRubric rubricEvaluation);

    GradedRubric toEntity(GradedRubricDTO gradedRubricDTO);

    List<GradedRubricDTO> toDTOList(List<GradedRubric> rubricEvaluations);

    List<GradedRubric> toEntityList(List<GradedRubricDTO> gradedRubricDTOS);
}
