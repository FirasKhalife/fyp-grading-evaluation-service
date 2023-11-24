package com.fypgrading.evaluationservice.service.mapper;

import com.fypgrading.evaluationservice.entity.Evaluation;
import com.fypgrading.evaluationservice.service.dto.EvaluationDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {GradedRubricMapper.class}
)
public interface EvaluationMapper {

    Evaluation toEntity(EvaluationDTO gradingDTO);

    EvaluationDTO toDTO(Evaluation grading);

    List<Evaluation> toEntityList(List<EvaluationDTO> gradingDTOs);

    List<EvaluationDTO> toDTOList(List<Evaluation> gradings);
}
