package com.rulex.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulex.controller.RuleController;
import com.rulex.engine.RuleEngine;
import com.rulex.engine.RuleEngine.TraceResult;
import com.rulex.engine.RuleEngine.ValidationResult;
import com.rulex.engine.TraceNode;
import com.rulex.exception.RuleEvaluationException;
import com.rulex.exception.RuleParseException;
import com.rulex.function.FunctionRegistry;
import com.rulex.service.RuleService;
import com.rulex.dto.EvaluateRequest;
import com.rulex.dto.ValidateRequest;
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

@WebMvcTest(RuleController.class)
@DisplayName("RuleController web layer tests")
class RuleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    RuleEngine ruleEngine;

    @MockBean
    FunctionRegistry functionRegistry;

    @MockBean
    RuleService ruleService;

    // ── /evaluate endpoint ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/rules/evaluate")
    class EvaluateEndpoint {

        @Test
        @DisplayName("Returns 200 with result=true for a passing rule")
        void evaluate_returnsTrue() throws Exception {
            when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(true);

            EvaluateRequest request = new EvaluateRequest("age > 18", Map.of("age", 25));

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(true))
                    .andExpect(jsonPath("$.rule").value("age > 18"));
        }

        @Test
        @DisplayName("Returns 200 with result=false for a failing rule")
        void evaluate_returnsFalse() throws Exception {
            when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(false);

            EvaluateRequest request = new EvaluateRequest("age > 18", Map.of("age", 10));

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(false))
                    .andExpect(jsonPath("$.rule").value("age > 18"));
        }

        @Test
        @DisplayName("Returns 400 when rule is blank")
        void evaluate_blankRule_returns400() throws Exception {
            EvaluateRequest request = new EvaluateRequest("", Map.of("age", 25));

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Returns 400 when context is null")
        void evaluate_nullContext_returns400() throws Exception {
            String json = "{\"rule\": \"age > 18\", \"context\": null}";

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Returns 400 when rule has a parse error")
        void evaluate_parseError_returns400() throws Exception {
            when(ruleEngine.evaluate(anyString(), anyMap()))
                    .thenThrow(new RuleParseException("age >>> 18", 1, 5, "extraneous input '>'"));

            EvaluateRequest request = new EvaluateRequest("age >>> 18", Map.of("age", 25));

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("PARSE_ERROR"))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Returns 422 when rule has an evaluation error")
        void evaluate_evaluationError_returns422() throws Exception {
            when(ruleEngine.evaluate(anyString(), anyMap()))
                    .thenThrow(new RuleEvaluationException("Cannot compare null values"));

            EvaluateRequest request = new EvaluateRequest("age > 18", Map.of());

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("EVALUATION_ERROR"))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Returns 400 when request body is malformed JSON")
        void evaluate_malformedJson_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 200 with trace when explain=true")
        void evaluate_withExplain_returnsTrace() throws Exception {
            TraceNode traceNode = TraceNode.leaf("age > 18", "COMPARISON", true, "25.0 > 18.0");
            when(ruleEngine.evaluateWithTrace(anyString(), anyMap()))
                    .thenReturn(new TraceResult(true, traceNode));

            EvaluateRequest request = new EvaluateRequest("age > 18", Map.of("age", 25));

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .param("explain", "true")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(true))
                    .andExpect(jsonPath("$.trace").exists())
                    .andExpect(jsonPath("$.trace.type").value("COMPARISON"))
                    .andExpect(jsonPath("$.trace.result").value(true))
                    .andExpect(jsonPath("$.trace.evaluated").value("25.0 > 18.0"));
        }

        @Test
        @DisplayName("Returns 200 without trace when explain is omitted")
        void evaluate_withoutExplain_noTrace() throws Exception {
            when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(true);

            EvaluateRequest request = new EvaluateRequest("age > 18", Map.of("age", 25));

            mockMvc.perform(post("/api/v1/rules/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(true))
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
    }

    // ── /validate endpoint ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/rules/validate")
    class ValidateEndpoint {

        @Test
        @DisplayName("Returns 200 with valid=true for a syntactically correct rule")
        void validate_validRule_returnsSuccess() throws Exception {
            when(ruleEngine.validate(anyString())).thenReturn(ValidationResult.success());

            ValidateRequest request = new ValidateRequest("age > 18 AND active = true");

            mockMvc.perform(post("/api/v1/rules/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.error").doesNotExist());
        }

        @Test
        @DisplayName("Returns 200 with valid=false and error message for invalid rule")
        void validate_invalidRule_returnsFailure() throws Exception {
            when(ruleEngine.validate(anyString()))
                    .thenReturn(ValidationResult.failure("Parse error at line 1:5 — extraneous input '>'"));

            ValidateRequest request = new ValidateRequest("age >>> 18");

            mockMvc.perform(post("/api/v1/rules/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.error").isNotEmpty());
        }

        @Test
        @DisplayName("Returns 400 when rule is blank")
        void validate_blankRule_returns400() throws Exception {
            ValidateRequest request = new ValidateRequest("   ");

            mockMvc.perform(post("/api/v1/rules/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Returns 400 when request body is missing")
        void validate_missingBody_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/rules/validate")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 200 with valid=true for complex nested rule")
        void validate_complexRule_returnsSuccess() throws Exception {
            when(ruleEngine.validate(anyString())).thenReturn(ValidationResult.success());

            ValidateRequest request = new ValidateRequest(
                    "(age > 18 AND score > 50) OR (vip = true AND status != 'banned')");

            mockMvc.perform(post("/api/v1/rules/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }
    }
}
