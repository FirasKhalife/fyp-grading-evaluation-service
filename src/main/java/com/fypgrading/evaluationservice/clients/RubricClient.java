package com.fypgrading.evaluationservice.clients;

import com.fypgrading.evaluationservice.evaluation.Assessment;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** rubric-service owns the criteria; a new draft takes a snapshot of them. */
@Component
public class RubricClient {

    private final RestClient http;

    public RubricClient(
            @Qualifier(ClientConfig.LOAD_BALANCED) RestClient.Builder loadBalanced,
            @Value("${assay.clients.rubric:http://rubric-service}") String base) {
        this.http = loadBalanced.baseUrl(base).build();
    }

    public Rubric rubric(Assessment assessment) {
        return Remote.call(
                "rubric service",
                () -> http.get()
                        .uri("/api/rubrics/{assessment}", assessment)
                        .retrieve()
                        .body(Rubric.class));
    }

    public record Rubric(Assessment assessment, String name, List<Criterion> criteria) {}

    public record Criterion(String id, int position, String name, int weight, Map<String, String> descriptions) {}
}
