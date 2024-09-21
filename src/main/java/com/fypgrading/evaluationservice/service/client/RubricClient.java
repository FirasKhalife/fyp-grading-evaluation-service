package com.fypgrading.evaluationservice.service.client;

import com.fypgrading.evaluationservice.service.dto.RubricDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
    name = "${services.rubric-service-name}",
    path = "/api",
    fallback = RubricFallback.class
)
public interface RubricClient {

    @GetMapping("/rubrics/")
    List<RubricDTO> getRubrics();

}
