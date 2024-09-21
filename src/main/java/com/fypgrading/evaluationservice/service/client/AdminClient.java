package com.fypgrading.evaluationservice.service.client;

import com.fypgrading.evaluationservice.service.dto.GradeIDsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "${services.admin-service-name}", path = "/api")
public interface AdminClient {

    @PostMapping("/")
    void submitGrade(@RequestBody GradeIDsDTO grade);

}
