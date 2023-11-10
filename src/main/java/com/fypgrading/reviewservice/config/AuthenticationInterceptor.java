package com.fypgrading.reviewservice.config;

import com.fypgrading.reviewservice.service.dto.AuthDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthenticationInterceptor implements HandlerInterceptor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String email = request.getHeader("email");
        String password = request.getHeader("password");

        AuthDTO authDTO = new AuthDTO();
        authDTO.setEmail(email);
        authDTO.setPassword(password);

        try {
            restTemplate.exchange(
                    "http://localhost:8081/api/auth/login",
                    HttpMethod.POST, new HttpEntity<>(authDTO), new ParameterizedTypeReference<>() {}, String.class
            );

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 401) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized");
                return false;
            }

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Unexpected error occurred");
            return false;
        }

        return true;
    }
}
