package com.fypgrading.evaluationservice.common.demo;

import com.fypgrading.evaluationservice.common.web.ProblemWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Requires the {@value DemoSession#HEADER} header on every {@code /api/**} and {@code /internal/**} request
 * (except the demo-copy endpoints, which take the session from the path) and binds it to the
 * thread. Runs after Spring Security, so an anonymous call gets 401 before it gets 400.
 */
@Component
public class DemoSessionFilter extends OncePerRequestFilter {

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> publicPaths;

    public DemoSessionFilter(@Value("${assay.demo.public-paths:}") List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !(path.startsWith("/api/") || path.startsWith("/internal/"))
                || path.startsWith("/internal/demo-sessions/")
                || publicPaths.stream().anyMatch(p -> matcher.match(p, path));
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<UUID> session = DemoSession.parse(request.getHeader(DemoSession.HEADER));
        if (session.isEmpty()) {
            ProblemWriter.write(
                    response,
                    HttpStatus.BAD_REQUEST,
                    "demo-session-required",
                    "Every request needs an X-Demo-Session header holding a random UUID.",
                    request.getRequestURI());
            return;
        }
        try (DemoSession.Binding ignored = DemoSession.bind(session.get())) {
            chain.doFilter(request, response);
        }
    }
}
