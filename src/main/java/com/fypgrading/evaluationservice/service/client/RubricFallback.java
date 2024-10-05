package com.fypgrading.evaluationservice.service.client;

import com.fypgrading.evaluationservice.service.dto.RubricDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RubricFallback implements RubricClient {

    @Override
    public List<RubricDTO> getRubrics() {
        return List.of();
    }
}
