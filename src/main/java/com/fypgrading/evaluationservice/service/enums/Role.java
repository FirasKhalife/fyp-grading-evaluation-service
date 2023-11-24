package com.fypgrading.evaluationservice.service.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Role {
    @JsonProperty("admin")
    ADMIN,
    @JsonProperty("jury_member")
    JURY_MEMBER,
    @JsonProperty("advisor")
    ADVISOR;
}
