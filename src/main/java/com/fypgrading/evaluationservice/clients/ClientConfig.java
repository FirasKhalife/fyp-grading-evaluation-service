package com.fypgrading.evaluationservice.clients;

import com.fypgrading.evaluationservice.common.demo.DemoSession;
import com.fypgrading.evaluationservice.common.security.CurrentUser;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Calls to other services go through Eureka (load-balanced names like {@code http://admin-service}), with
 * short timeouts, and carry the caller's token and demo session so the other side applies the same rules.
 */
@Configuration
public class ClientConfig {

    public static final String LOAD_BALANCED = "loadBalanced";

    /** The default builder stays plain: Eureka's own client uses it to reach the registry. */
    @Bean
    @Primary
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    @Qualifier(LOAD_BALANCED)
    RestClient.Builder loadBalancedRestClientBuilder() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory).requestInterceptor(forwardCallerContext());
    }

    static ClientHttpRequestInterceptor forwardCallerContext() {
        return (request, body, execution) -> {
            CurrentUser.find().ifPresent(u -> request.getHeaders().setBearerAuth(u.bearerToken()));
            DemoSession.current().ifPresent(s -> request.getHeaders().set(DemoSession.HEADER, s.toString()));
            if (!request.getHeaders().containsHeader(HttpHeaders.ACCEPT)) {
                request.getHeaders().set(HttpHeaders.ACCEPT, "application/json");
            }
            return execution.execute(request, body);
        };
    }
}
