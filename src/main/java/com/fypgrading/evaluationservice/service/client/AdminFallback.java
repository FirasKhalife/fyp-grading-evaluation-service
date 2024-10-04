package com.fypgrading.evaluationservice.service.client;

import com.fypgrading.evaluationservice.service.dto.GradeIdDTO;
import org.springframework.stereotype.Component;

@Component
public class AdminFallback implements AdminClient {

    @Override
    public void submitGrade(GradeIdDTO grade) {}
}
