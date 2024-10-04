package com.fypgrading.evaluationservice.service.mapper;

import com.fypgrading.evaluationservice.entity.GradedRubric;
import com.fypgrading.evaluationservice.service.dto.GradedRubricDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GradedRubricMapper extends EntityMapper<GradedRubric, GradedRubricDTO> {
}
