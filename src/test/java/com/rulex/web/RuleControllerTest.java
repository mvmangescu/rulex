package com.rulex.web;

import com.rulex.controller.RuleController;
import com.rulex.dto.CreateRuleRequest;
import com.rulex.dto.RuleResponse;
import com.rulex.dto.UpdateRuleRequest;
import com.rulex.engine.RuleEngine;
import com.rulex.engine.RuleEngine.TraceResult;
import com.rulex.engine.TraceNode;
import com.rulex.service.RuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RuleController.class)
@DisplayName("RuleController tests")
class RuleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RuleService ruleService;

    @MockBean
    RuleEngine ruleEngine;

    private RuleResponse sampleRule() {
        return new RuleResponse(1L, "senior-check", "age > 60", null);
    }

    @Nested
    @DisplayName("POST /api/v1/rules")
    class CreateEndpoint {

        @Test
        @DisplayName("Returns 201 with Location header when creating a new rule")
        void create_newRule_returns201() throws Exception {
            when(ruleService.create(any(CreateRuleRequest.class))).thenReturn(sampleRule());

            mockMvc.perform(post("/api/v1/rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"senior-check\", \"expression\": \"age > 60\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/rules/1"))
                    .andExpect(jsonPath("$.name").value("senior-check"))
                    .andExpect(jsonPath("$.expression").value("age > 60"));
        }

        @Test
        @DisplayName("Returns 400 when expression is blank")
        void create_blankExpression_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"senior-check\", \"expression\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/rules/{id}")
    class UpdateEndpoint {

        @Test
        @DisplayName("Returns 200 when updating an existing rule")
        void update_existingRule_returns200() throws Exception {
            RuleResponse updated = new RuleResponse(1L, "senior-check", "age > 65", null);
            when(ruleService.update(anyLong(), any(UpdateRuleRequest.class))).thenReturn(updated);

            mockMvc.perform(put("/api/v1/rules/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"expression\": \"age > 65\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expression").value("age > 65"));
        }

        @Test
        @DisplayName("Returns 200 when renaming a rule")
        void update_renameRule_returns200() throws Exception {
            RuleResponse renamed = new RuleResponse(1L, "elder-check", "age > 65", null);
            when(ruleService.update(anyLong(), any(UpdateRuleRequest.class))).thenReturn(renamed);

            mockMvc.perform(put("/api/v1/rules/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"elder-check\", \"expression\": \"age > 65\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("elder-check"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rules/{id}")
    class GetEndpoint {

        @Test
        @DisplayName("Returns 200 with rule when found")
        void get_existingRule_returns200() throws Exception {
            when(ruleService.findById(1L)).thenReturn(Optional.of(sampleRule()));

            mockMvc.perform(get("/api/v1/rules/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("senior-check"))
                    .andExpect(jsonPath("$.expression").value("age > 60"));
        }

        @Test
        @DisplayName("Returns 404 when rule does not exist")
        void get_missingRule_returns404() throws Exception {
            when(ruleService.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/rules/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rules")
    class ListEndpoint {

        @Test
        @DisplayName("Returns 200 with all rules")
        void list_returnsAllRules() throws Exception {
            when(ruleService.findAll()).thenReturn(List.of(sampleRule()));

            mockMvc.perform(get("/api/v1/rules"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("senior-check"));
        }

        @Test
        @DisplayName("Returns 200 with empty array when no rules exist")
        void list_empty_returnsEmptyArray() throws Exception {
            when(ruleService.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/rules"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/rules/{id}")
    class DeleteEndpoint {

        @Test
        @DisplayName("Returns 204 when rule is deleted")
        void delete_existingRule_returns204() throws Exception {
            when(ruleService.deleteById(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/v1/rules/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Returns 404 when rule does not exist")
        void delete_missingRule_returns404() throws Exception {
            when(ruleService.deleteById(99L)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/rules/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/rules/{id}/evaluate")
    class EvaluateEndpoint {

        @Test
        @DisplayName("Returns 200 with result when rule exists")
        void evaluate_existingRule_returnsResult() throws Exception {
            when(ruleService.findById(1L)).thenReturn(Optional.of(sampleRule()));
            when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(true);

            mockMvc.perform(post("/api/v1/rules/1/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"age\": 65}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(true))
                    .andExpect(jsonPath("$.rule").value("age > 60"));
        }

        @Test
        @DisplayName("Returns 200 with trace when explain=true")
        void evaluate_withExplain_returnsTrace() throws Exception {
            when(ruleService.findById(1L)).thenReturn(Optional.of(sampleRule()));
            TraceNode trace = TraceNode.leaf("age > 60", "COMPARISON", true, "65.0 > 60.0");
            when(ruleEngine.evaluateWithTrace(anyString(), anyMap()))
                    .thenReturn(new TraceResult(true, trace));

            mockMvc.perform(post("/api/v1/rules/1/evaluate")
                            .param("explain", "true")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"age\": 65}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(true))
                    .andExpect(jsonPath("$.trace.type").value("COMPARISON"))
                    .andExpect(jsonPath("$.trace.evaluated").value("65.0 > 60.0"));
        }

        @Test
        @DisplayName("Returns 404 when rule does not exist")
        void evaluate_missingRule_returns404() throws Exception {
            when(ruleService.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/rules/99/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"age\": 65}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }
}
