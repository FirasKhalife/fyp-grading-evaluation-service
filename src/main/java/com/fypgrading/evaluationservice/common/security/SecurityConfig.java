package com.fypgrading.evaluationservice.common.security;

import com.fypgrading.evaluationservice.common.web.ProblemWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every API call needs a valid Keycloak token; roles come from {@code realm_access.roles}. Finer rules are
 * {@code @PreAuthorize} on the controllers. Actuator is limited to health, info and prometheus, and the
 * gateway doesn't route it, so it's only reachable on the internal network.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Only tokens issued to our own clients ({@code azp}); Boot checks the issuer from {@code issuer-uri}. */
    @Bean
    OAuth2TokenValidator<Jwt> authorizedParty(
            @Value("${assay.security.authorized-parties:assay-spa}") List<String> clients) {
        return new AuthorizedParty(clients);
    }

    @Bean
    SecurityFilterChain api(HttpSecurity http, @Value("${assay.security.public-paths:}") String[] publicPaths)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus")
                            .permitAll();
                    auth.requestMatchers("/v3/api-docs/**").permitAll();
                    if (publicPaths.length > 0) {
                        auth.requestMatchers(publicPaths).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRoles()))
                        .authenticationEntryPoint((req, res, e) -> ProblemWriter.write(
                                res,
                                HttpStatus.UNAUTHORIZED,
                                "unauthenticated",
                                "Sign in again: the token is missing or expired.",
                                req.getRequestURI())))
                .exceptionHandling(ex -> ex.accessDeniedHandler((req, res, e) -> ProblemWriter.write(
                        res, HttpStatus.FORBIDDEN, "forbidden", "Your role can't do this.", req.getRequestURI())));
        return http.build();
    }

    static JwtAuthenticationConverter keycloakRoles() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RealmRoles());
        return converter;
    }

    /** Maps {@code realm_access.roles: [ADMIN]} to the authority {@code ROLE_ADMIN}. */
    static final class RealmRoles implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Object realmAccess = jwt.getClaims().get("realm_access");
            if (!(realmAccess instanceof Map<?, ?> map) || !(map.get("roles") instanceof Collection<?> roles)) {
                return List.of();
            }
            return roles.stream()
                    .map(String::valueOf)
                    .filter(r -> r.equals(Roles.ADMIN) || r.equals(Roles.REVIEWER) || r.equals(Roles.STUDENT))
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
        }
    }
}
