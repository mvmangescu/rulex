package com.rulex.dto;

import com.rulex.engine.ParseTreeNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a dry-run parse — expression is syntactically valid, no evaluation performed")
public record DryRunResponse(

        @Schema(description = "The rule expression that was parsed", example = "age > 18 AND active = true")
        String rule,

        @Schema(description = "Structural parse tree of the expression")
        ParseTreeNode parseTree) {
}
