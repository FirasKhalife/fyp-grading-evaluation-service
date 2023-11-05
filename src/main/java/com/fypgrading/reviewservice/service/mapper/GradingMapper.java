package com.fypgrading.reviewservice.service.mapper;

import com.fypgrading.reviewservice.entity.Grading;
import com.fypgrading.reviewservice.service.dto.GradingDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface GradingMapper {

    Grading toEntity(GradingDTO gradingDTO);

    GradingDTO toDTO(Grading grading);

    List<Grading> toEntityList(List<GradingDTO> gradingDTOs);

    List<GradingDTO> toDTOList(List<Grading> gradings);
}
