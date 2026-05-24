package com.rulex.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulex.controller.RuleEngineController;
import com.rulex.dto.EvaluateRequest;
import com.rulex.dto.ValidateRequest;
import com.rulex.engine.RuleEngine;
import com.rulex.engine.RuleEngine.TraceResult;
import com.rulex.engine.RuleEngine.ValidationResult;
import com.rulex.engine.TraceNode;
import com.rulex.exception.RuleEvaluationException;
import com.rulex.exception.RuleParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RuleEngineController.class)
@DisplayName("RuleEngineController tests")
class RuleEngineControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    RuleEngine ruleEngine;

    @Nested
    @DisplayName("POST /api/v1/rules/evaluate")
    class EvaluateEndpoint {

        @Test
        @DisplayName("Returns 200 with result=true for a passing rule")
        void evaluate_returnsTrue() throws Exception {
            when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(true);

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EvaluateRequest("age > 18", Map.of("age", 25)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(true))
                    .andExpect(jsonPath("$.rule").value("age > 18"));
        }

        @Test
        @DisplayName("Returns 200 with result=false for a failing rule")
        void evaluate_returnsFalse() throws Exception {
            when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(false);

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EvaluateRequest("age > 18", Map.of("age", 10)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(false));
        }

        @Test
        @DisplayName("Returns 400 when rule is blank")
        void evaluate_blankRule_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EvaluateRequest("", Map.of("age", 25)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Returns 400 when context is null")
        void evaluate_nullContext_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rule\": \"age > 18\", \"context\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Returns 400 when rule has a parse error")
        void evaluate_parseError_returns400() throws Exception {
            when(ruleEngine.evaluate(anyString(), anyMap()))
                    .thenThrow(new RuleParseException("age >>> 18", 1, 5, "extraneous input '>'"));

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EvaluateRequest("age >>> 18", Map.of("age", 25)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("PARSE_ERROR"))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("Returns 422 when rule has an evaluation error")
        void evaluate_evaluationError_returns422() throws Exception {
            when(ruleEngine.evaluate(anyString(), anyMap()))
                    .thenThrow(new RuleEvaluationException("Cannot compare null values"));

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EvaluateRequest("age > 18", Map.of()))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("EVALUATION_ERROR"));
        }

        @Test
        @DisplayName("Returns 200 with trace when explain=true")
        void evaluate_withExplain_returnsTrace() throws Exception {
            TraceNode traceNode = TraceNode.leaf("age > 18", "COMPARISON", true, "25.0 > 18.0");
            when(ruleEngine.evaluateWithTrace(anyString(), anyMap()))
                    .thenReturn(new TraceResult(true, traceNode));

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .param("explain", "true")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EvaluateRequest("age > 18", Map.of("age", 25)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(true))
                    .andExpect(jsonPath("$.trace.type").value("COMPARISON"))
                    .andExpect(jsonPath("$.trace.evaluated").value("25.0 > 18.0"));
        }

        @Test
        @DisplayName("Returns 200 without trace when explain is omitted")
        void evaluate_withoutExplain_noTrace() throws Exception {
            when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(true);

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EvaluateRequest("age > 18", Map.of("age", 25)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/rules/validate")
    class ValidateEndpoint {

        @Test
        @DisplayName("Returns 200 with valid=true for a correct rule")
        void validate_validRule_returnsSuccess() throws Exception {
            when(ruleEngine.validate(anyString())).thenReturn(ValidationResult.success());

            mockMvc.perform(post("/api/v1/rules/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ValidateRequest("age > 18 AND active = true"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.error").doesNotExist());
        }

        @Test
        @DisplayName("Returns 200 with valid=false and error for invalid rule")
        void validate_invalidRule_returnsFailure() throws Exception {
            when(ruleEngine.validate(anyString()))
                    .thenReturn(ValidationResult.failure("Parse error at line 1:5 — extraneous input '>'"));

            mockMvc.perform(post("/api/v1/rules/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ValidateRequest("age >>> 18"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.error").isNotEmpty());
        }

        @Test
        @DisplayName("Returns 400 when rule is blank")
        void validate_blankRule_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/rules/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ValidateRequest("   "))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

}
