package com.fypgrading.reviewservice.service.mapper;

import com.fypgrading.reviewservice.entity.RubricGrading;
import com.fypgrading.reviewservice.service.dto.RubricGradingDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface RubricGradingMapper {

    RubricGradingDTO toDTO(RubricGrading rubricGrading);

    RubricGrading toEntity(RubricGradingDTO rubricGradingDTO);

    List<RubricGradingDTO> toDTOList(List<RubricGrading> rubricGradings);

    List<RubricGrading> toEntityList(List<RubricGradingDTO> rubricGradingDTOs);
}
