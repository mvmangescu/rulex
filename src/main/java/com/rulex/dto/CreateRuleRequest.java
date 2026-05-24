package com.rulex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRuleRequest(

        @NotBlank
        @Size(max = 256)
        String name,

        @NotBlank
        @Size(max = 4096)
        String expression,

        @Size(max = 1024)
        String description) {
}
