package com.fypgrading.evaluationservice.common.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/** Builds problem details, and writes them from filters and security handlers (outside Spring MVC). */
public final class ProblemWriter {

    public static final String TYPE_BASE = "https://assay.dev/problems/";

    private ProblemWriter() {
    }

    public static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_BASE + code));
        problem.setProperty("code", code);
        return problem;
    }

    public static void write(HttpServletResponse response, HttpStatus status, String code, String detail,
            String instance) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"" + TYPE_BASE + code + "\",\"title\":\"" + status.getReasonPhrase()
                + "\",\"status\":" + status.value() + ",\"detail\":\"" + escape(detail) + "\",\"instance\":\""
                + escape(instance) + "\",\"code\":\"" + code + "\"}");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
