package com.fypgrading.reviewservice.config.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fypgrading.adminservice.exceptions.ExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;


@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {

        try {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            ExceptionResponse exResponse =
                    new ExceptionResponse(HttpStatus.UNAUTHORIZED, "Unauthorized");

            final ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), exResponse);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set user authentication");
        }
    }
}