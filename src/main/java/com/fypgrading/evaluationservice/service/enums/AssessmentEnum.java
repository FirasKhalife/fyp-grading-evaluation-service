package com.fypgrading.evaluationservice.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AssessmentEnum {
    PROPOSAL_PRESENTATION,
    PROGRESS_REPORT,
    ADVISOR_ASSESSMENT,
    FINAL_REPORT,
    FINAL_PRESENTATION;

    public Long getEnumId() {
        return ordinal() + 1L;
    }
}
