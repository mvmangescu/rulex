package com.rulex.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BatchEvaluateRequest(
        @NotEmpty(message = "Rules list must not be empty")
        @Size(max = 50, message = "Batch size must not exceed 50 rules")
        List<@Valid EvaluateRequest> rules
) {}
