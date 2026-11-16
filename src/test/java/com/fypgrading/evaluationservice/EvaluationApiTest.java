package com.fypgrading.evaluationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fypgrading.evaluationservice.clients.AdminClient;
import com.fypgrading.evaluationservice.clients.AdminClient.Assignment;
import com.fypgrading.evaluationservice.clients.AdminClient.GradeReceipt;
import com.fypgrading.evaluationservice.clients.AdminClient.GradeSubmission;
import com.fypgrading.evaluationservice.clients.RubricClient;
import com.fypgrading.evaluationservice.common.web.ApiException;
import com.fypgrading.evaluationservice.demo.DemoCopyService;
import com.fypgrading.evaluationservice.evaluation.Assessment;
import com.fypgrading.evaluationservice.evaluation.EvaluationRepository;
import com.fypgrading.evaluationservice.evaluation.ReviewerRole;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(TestChannelBinderConfiguration.class)
class EvaluationApiTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    static final UUID JANE = UUID.fromString("00000000-0000-4000-a000-000000000002");
    static final String TEAM = UUID.randomUUID().toString();
    static final String PATH = "/api/my-evaluations/" + TEAM + "/PROPOSAL_PRESENTATION";

    @Autowired
    MockMvc mvc;

    @Autowired
    OutputDestination output;

    @Autowired
    EvaluationRepository evaluations;

    @Autowired
    DemoCopyService copies;

    @MockitoBean
    AdminClient admin;

    @MockitoBean
    RubricClient rubrics;

    UUID session = UUID.randomUUID();

    static JwtRequestPostProcessor jane() {
        return jwt().jwt(j -> j.subject(JANE.toString()).claim("name", "Jane Smith"))
                .authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"));
    }

    static JwtRequestPostProcessor coordinator() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @BeforeEach
    void stubs() {
        output.clear();
        when(admin.assignment(eq(TEAM), eq(Assessment.PROPOSAL_PRESENTATION), any()))
                .thenReturn(new Assignment(
                        TEAM,
                        "Beta",
                        Assessment.PROPOSAL_PRESENTATION,
                        ReviewerRole.JURY,
                        true,
                        new AdminClient.Person(JANE.toString(), "Jane", "Smith"),
                        Instant.now().plusSeconds(86400),
                        false,
                        0,
                        3));
        when(rubrics.rubric(Assessment.PROPOSAL_PRESENTATION))
                .thenReturn(new RubricClient.Rubric(
                        Assessment.PROPOSAL_PRESENTATION,
                        "Proposal presentation",
                        List.of(
                                new RubricClient.Criterion("c1", 1, "Objectives", 20, Map.of("4", "a")),
                                new RubricClient.Criterion("c2", 2, "Requirements", 40, Map.of("4", "a")),
                                new RubricClient.Criterion("c3", 3, "Slides", 40, Map.of("4", "a")))));
        when(admin.submitGrade(any())).thenReturn(new GradeReceipt(1, 3));
    }

    private static String form(Integer l1, Integer l2, Integer l3) {
        return """
                {"criteria": [{"criterionId": "c1", "level": %s, "comment": "Clear objectives."},
                              {"criterionId": "c2", "level": %s}, {"criterionId": "c3", "level": %s}],
                 "overallComment": "Good start."}""".formatted(l1, l2, l3);
    }

    @Test
    void aReviewerGetsABlankFormThenDraftsThenSubmitsAndIsLockedOut() throws Exception {
        mvc.perform(get(PATH).header("X-Demo-Session", session).with(jane()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.criteriaCount").value(3));

        mvc.perform(put(PATH)
                        .header("X-Demo-Session", session)
                        .with(jane())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(form(4, 3, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.scoredCount").value(2))
                .andExpect(jsonPath("$.score").value(50.0));

        mvc.perform(post(PATH + "/submit").header("X-Demo-Session", session).with(jane()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("criteria-unscored"))
                .andExpect(jsonPath("$.errors[0].field").value("criteria[c3].level"));

        mvc.perform(post(PATH + "/submit")
                        .header("X-Demo-Session", session)
                        .with(jane())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(form(4, 3, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.score").value(70.0));

        ArgumentCaptor<GradeSubmission> grade = ArgumentCaptor.forClass(GradeSubmission.class);
        verify(admin).submitGrade(grade.capture());
        assertThat(grade.getValue().score()).isEqualTo(70.0);
        assertThat(grade.getValue().reviewerId()).isEqualTo(JANE.toString());

        // Retrying the submit is safe and doesn't hand the grade over twice.
        mvc.perform(post(PATH + "/submit").header("X-Demo-Session", session).with(jane()))
                .andExpect(status().isOk());
        verify(admin, times(1)).submitGrade(any());

        mvc.perform(put(PATH)
                        .header("X-Demo-Session", session)
                        .with(jane())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(form(1, 1, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("evaluation-submitted"));
    }

    @Test
    void changingALevelIsRecordedAndAnnouncedToTheActivityFeed() throws Exception {
        mvc.perform(put(PATH)
                        .header("X-Demo-Session", session)
                        .with(jane())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(form(2, null, null)))
                .andExpect(status().isOk());
        Message<byte[]> created = output.receive(2000, "assay.activity");
        assertThat(new String(created.getPayload(), StandardCharsets.UTF_8)).contains("DRAFT_SAVED");

        mvc.perform(put(PATH)
                        .header("X-Demo-Session", session)
                        .with(jane())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(form(3, null, null)))
                .andExpect(status().isOk());
        Message<byte[]> changed = output.receive(2000, "assay.activity");
        assertThat(new String(changed.getPayload(), StandardCharsets.UTF_8))
                .contains("LEVEL_CHANGED")
                .contains("changed Objectives 2 → 3 in the proposal presentation draft for Team Beta.");

        String id = evaluations.findByDemoSession(session.toString()).getFirst().getId();
        mvc.perform(get("/api/evaluations/" + id + "/history")
                        .header("X-Demo-Session", session)
                        .with(coordinator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("LEVEL_CHANGED"))
                .andExpect(jsonPath("$[0].from").value(2))
                .andExpect(jsonPath("$[0].to").value(3))
                .andExpect(jsonPath("$[0].criterionName").value("Objectives"));
        mvc.perform(get("/api/evaluations/" + id + "/history")
                        .header("X-Demo-Session", session)
                        .with(jane()))
                .andExpect(status().isForbidden());
    }

    @Test
    void aReviewerWithoutTheRoleOnTheTeamCantGrade() throws Exception {
        when(admin.assignment(eq(TEAM), eq(Assessment.PROPOSAL_PRESENTATION), any()))
                .thenReturn(new Assignment(
                        TEAM,
                        "Beta",
                        Assessment.PROPOSAL_PRESENTATION,
                        ReviewerRole.JURY,
                        false,
                        new AdminClient.Person(JANE.toString(), "Jane", "Smith"),
                        null,
                        false,
                        0,
                        3));
        mvc.perform(get(PATH).header("X-Demo-Session", session).with(jane()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("not-assigned"));
        mvc.perform(get(PATH).header("X-Demo-Session", session).with(coordinator()))
                .andExpect(status().isForbidden());
    }

    @Test
    void ifAdminServiceIsDownTheEvaluationStaysAnEditableDraft() throws Exception {
        when(admin.submitGrade(any())).thenThrow(ApiException.unavailable("down"));
        mvc.perform(post(PATH + "/submit")
                        .header("X-Demo-Session", session)
                        .with(jane())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(form(4, 4, 4)))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(get(PATH).header("X-Demo-Session", session).with(jane()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void theSeedCopyIsPrivateToEachVisitor() {
        UUID other = UUID.randomUUID();
        copies.replace(session);
        copies.replace(other);
        copies.replace(session);
        long mine = evaluations.findByDemoSession(session.toString()).size();
        assertThat(mine).isGreaterThan(50);
        assertThat(evaluations.findByDemoSession(other.toString())).hasSize((int) mine);
        copies.delete(other);
        assertThat(evaluations.findByDemoSession(other.toString())).isEmpty();
        assertThat(evaluations.findByDemoSession(session.toString())).hasSize((int) mine);
    }
}
