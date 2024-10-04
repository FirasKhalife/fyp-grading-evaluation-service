package com.fypgrading.evaluationservice.service.mapper;

import com.fypgrading.evaluationservice.entity.Evaluation;
import com.fypgrading.evaluationservice.service.dto.EvaluationDTO;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        uses = { GradedRubricMapper.class }
)
public interface EvaluationMapper extends EntityMapper<Evaluation, EvaluationDTO> {
}
