package com.fypgrading.evaluationservice.common.security;

/** Keycloak realm roles (in {@code realm_access.roles}). Jury and advisor are per-team roles, not these. */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String REVIEWER = "REVIEWER";
    public static final String STUDENT = "STUDENT";

    private Roles() {
    }
}
