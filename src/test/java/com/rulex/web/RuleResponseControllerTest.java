package com.rulex.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulex.controller.RuleController;
import com.rulex.engine.RuleEngine;
import com.rulex.engine.RuleEngine.TraceResult;
import com.rulex.engine.TraceNode;
import com.rulex.function.FunctionRegistry;
import com.rulex.dto.RuleResponse;
import com.rulex.service.RuleService;

@WebMvcTest(RuleController.class)
@DisplayName("Named rules web layer tests")
class RuleResponseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RuleService ruleService;

    @MockBean
    private RuleEngine ruleEngine;

    @MockBean
    private FunctionRegistry functionRegistry;

    private RuleResponse sampleRule() {
        return new RuleResponse(1L, "senior-check", "age > 60", null);
    }

    // ── POST /named ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/rules/named")
    class CreateEndpoint {

        @Test
        @DisplayName("Returns 201 with Location header when creating a new rule")
        void create_newRule_returns201() throws Exception {
            when(ruleService.create(any(RuleResponse.class))).thenReturn(sampleRule());

            mockMvc.perform(post("/api/v1/rules/named")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"senior-check\", \"expression\": \"age > 60\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/rules/named/senior-check"))
                    .andExpect(jsonPath("$.name").value("senior-check"))
                    .andExpect(jsonPath("$.expression").value("age > 60"));
        }

        @Test
        @DisplayName("Returns 400 when expression is blank")
        void create_blankExpression_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/rules/named")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"senior-check\", \"expression\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    // ── PUT /named/{name} ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/rules/named/{name}")
    class UpdateEndpoint {

        @Test
        @DisplayName("Returns 200 when updating an existing rule")
        void update_existingRule_returns200() throws Exception {
            RuleResponse updated = new RuleResponse(1L, "senior-check", "age > 65", null);
            when(ruleService.update(anyString(), any(RuleResponse.class))).thenReturn(updated);

            mockMvc.perform(put("/api/v1/rules/named/senior-check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"senior-check\", \"expression\": \"age > 65\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expression").value("age > 65"));
        }

        @Test
        @DisplayName("Returns 400 when expression is blank")
        void update_blankExpression_returns400() throws Exception {
            mockMvc.perform(put("/api/v1/rules/named/senior-check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"senior-check\", \"expression\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    // ── GET /named/{name} ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/rules/named/{name}")
    class GetEndpoint {

        @Test
        @DisplayName("Returns 200 with rule when found")
        void get_existingRule_returns200() throws Exception {
            when(ruleService.findByName("senior-check")).thenReturn(Optional.of(sampleRule()));

            mockMvc.perform(get("/api/v1/rules/named/senior-check"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("senior-check"))
                    .andExpect(jsonPath("$.expression").value("age > 60"));
        }

        @Test
        @DisplayName("Returns 404 when rule does not exist")
        void get_missingRule_returns404() throws Exception {
            when(ruleService.findByName("unknown")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/rules/named/unknown"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /named ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/rules/named")
    class ListEndpoint {

        @Test
        @DisplayName("Returns 200 with all rules")
        void list_returnsAllRules() throws Exception {
            when(ruleService.findAll()).thenReturn(List.of(sampleRule()));

            mockMvc.perform(get("/api/v1/rules/named"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("senior-check"));
        }

        @Test
        @DisplayName("Returns 200 with empty array when no rules exist")
        void list_empty_returnsEmptyArray() throws Exception {
            when(ruleService.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/rules/named"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ── DELETE /named/{name} ──────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/rules/named/{name}")
    class DeleteEndpoint {

        @Test
        @DisplayName("Returns 204 when rule is deleted")
        void delete_existingRule_returns204() throws Exception {
            when(ruleService.delete("senior-check")).thenReturn(true);

            mockMvc.perform(delete("/api/v1/rules/named/senior-check"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Returns 404 when rule does not exist")
        void delete_missingRule_returns404() throws Exception {
            when(ruleService.delete("unknown")).thenReturn(false);

            mockMvc.perform(delete("/api/v1/rules/named/unknown"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /named/{name}/evaluate ───────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/rules/named/{name}/evaluate")
    class EvaluateNamedEndpoint {

        @Test
        @DisplayName("Returns 200 with result when rule exists")
        void evaluate_existingRule_returnsResult() throws Exception {
            when(ruleService.findByName("senior-check")).thenReturn(Optional.of(sampleRule()));
            when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(true);

            mockMvc.perform(post("/api/v1/rules/named/senior-check/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"age\": 65}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(true))
                    .andExpect(jsonPath("$.rule").value("age > 60"));
        }

        @Test
        @DisplayName("Returns 200 with trace when explain=true")
        void evaluate_withExplain_returnsTrace() throws Exception {
            when(ruleService.findByName("senior-check")).thenReturn(Optional.of(sampleRule()));
            TraceNode trace = TraceNode.leaf("age > 60", "COMPARISON", true, "65.0 > 60.0");
            when(ruleEngine.evaluateWithTrace(anyString(), anyMap()))
                    .thenReturn(new TraceResult(true, trace));

            mockMvc.perform(post("/api/v1/rules/named/senior-check/evaluate")
                            .param("explain", "true")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"age\": 65}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(true))
                    .andExpect(jsonPath("$.trace.type").value("COMPARISON"))
                    .andExpect(jsonPath("$.trace.evaluated").value("65.0 > 60.0"));
        }

        @Test
        @DisplayName("Returns 404 when named rule does not exist")
        void evaluate_missingRule_returns404() throws Exception {
            when(ruleService.findByName("unknown")).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/rules/named/unknown/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"age\": 65}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }
}
