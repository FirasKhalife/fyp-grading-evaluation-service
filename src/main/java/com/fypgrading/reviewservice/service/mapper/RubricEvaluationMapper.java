package com.fypgrading.reviewservice.service.mapper;

import com.fypgrading.reviewservice.entity.RubricEvaluation;
import com.fypgrading.reviewservice.service.dto.RubricEvaluationDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface RubricEvaluationMapper {

    RubricEvaluationDTO toDTO(RubricEvaluation rubricEvaluation);

    RubricEvaluation toEntity(RubricEvaluationDTO rubricEvaluationDTO);

    List<RubricEvaluationDTO> toDTOList(List<RubricEvaluation> rubricEvaluations);

    List<RubricEvaluation> toEntityList(List<RubricEvaluationDTO> rubricEvaluationDTOs);
}
