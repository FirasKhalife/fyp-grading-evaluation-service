package com.fypgrading.reviewservice.service.mapper;

import com.fypgrading.reviewservice.entity.Evaluation;
import com.fypgrading.reviewservice.service.dto.EvaluationDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EvaluationMapper {

    Evaluation toEntity(EvaluationDTO gradingDTO);

    EvaluationDTO toDTO(Evaluation grading);

    List<Evaluation> toEntityList(List<EvaluationDTO> gradingDTOs);

    List<EvaluationDTO> toDTOList(List<Evaluation> gradings);
}
