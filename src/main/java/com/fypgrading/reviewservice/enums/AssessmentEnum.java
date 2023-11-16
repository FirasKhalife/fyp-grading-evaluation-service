package com.fypgrading.reviewservice.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum AssessmentEnum {

    @JsonProperty("ORAL_PROPOSAL")
    ORAL_PROPOSAL,

    @JsonProperty("PROGRESS")
    PROGRESS,

    @JsonProperty("PROGRESS_REPORT")
    ADVISOR,

    @JsonProperty("FINAL_REPORT")
    FINAL_REPORT,

    @JsonProperty("FINAL_PRESENTATION")
    FINAL_PRESENTATION;
}
