package com.fypgrading.evaluationservice.service.client;

import com.fypgrading.evaluationservice.service.dto.GradeIDsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Primary
@FeignClient(
    name = "${services.admin-service-name}",
    path = "/api",
    fallback = AdminFallback.class
)
public interface AdminClient {

    @PostMapping("/")
    void submitGrade(@RequestBody GradeIDsDTO grade);

}
