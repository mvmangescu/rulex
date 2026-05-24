package com.rulex.dto;

import jakarta.validation.constraints.Size;

public record UpdateRuleRequest(
        @Size(max = 256)
        String name,

        @Size(max = 4096)
        String expression,

        @Size(max = 1024)
        String description) {
}
