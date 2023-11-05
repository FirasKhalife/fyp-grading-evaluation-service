package com.fypgrading.reviewservice.config;

import com.fypgrading.reviewservice.service.dto.AuthDTO;
import com.fypgrading.reviewservice.service.dto.ReviewerDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
        ResponseEntity<ReviewerDTO> reviewerResponse = restTemplate.exchange(
                "http://localhost:8080/api/auth/login",
                HttpMethod.GET, new HttpEntity<>(authDTO), new ParameterizedTypeReference<>() {
                }
        );
        if (reviewerResponse.getStatusCode().isError()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
            return false; // Stop further processing of the request
        }

        return true; // Proceed with the request
    }
}
