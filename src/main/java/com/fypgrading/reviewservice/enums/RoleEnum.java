package com.fypgrading.reviewservice.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public enum RoleEnum implements GrantedAuthority {

    @JsonProperty("ADMIN")
    ADMIN,

    @JsonProperty("JURY_MEMBER")
    JURY_MEMBER,

    @JsonProperty("ADVISOR")
    ADVISOR,

    @JsonProperty("ALL")
    ALL;

    @Override
    public String getAuthority() {
        return name();
    }

}
