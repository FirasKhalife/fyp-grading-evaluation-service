package com.fypgrading.evaluationservice.service.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AssessmentEnum {

    @JsonProperty("ORAL_PROPOSAL")
    ORAL_PROPOSAL,

    @JsonProperty("PROGRESS")
    PROGRESS,

    @JsonProperty("ADVISOR")
    ADVISOR,

    @JsonProperty("FINAL_REPORT")
    FINAL_REPORT,

    @JsonProperty("FINAL_PRESENTATION")
    FINAL_PRESENTATION
}
