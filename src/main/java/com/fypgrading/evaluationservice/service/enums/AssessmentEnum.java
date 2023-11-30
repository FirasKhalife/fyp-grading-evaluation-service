package com.fypgrading.evaluationservice.service.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AssessmentEnum {

    @JsonProperty("PROPOSAL_PRESENTATION")
    PROPOSAL_PRESENTATION,

    @JsonProperty("PROGRESS_REPORT")
    PROGRESS_REPORT,

    @JsonProperty("ADVISOR_ASSESSMENT")
    ADVISOR_ASSESSMENT,

    @JsonProperty("FINAL_REPORT")
    FINAL_REPORT,

    @JsonProperty("FINAL_PRESENTATION")
    FINAL_PRESENTATION;

    public Integer getEnumId() {
        return ordinal() + 1;
    }
}
