package com.fypgrading.evaluationservice.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class RealmRolesTest {

    private static Jwt token(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "none").subject("s")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void readsRolesFromRealmAccessAndIgnoresKeycloakDefaults() {
        Jwt jwt = token(Map.of("realm_access", Map.of("roles", List.of("ADMIN", "REVIEWER", "offline_access"))));
        assertThat(new SecurityConfig.RealmRoles().convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_REVIEWER");
    }

    @Test
    void theOldRealmClaimGrantsNothing() {
        Jwt jwt = token(Map.of("realm", Map.of("roles", List.of("ADMIN"))));
        assertThat(new SecurityConfig.RealmRoles().convert(jwt)).isEmpty();
    }
}
