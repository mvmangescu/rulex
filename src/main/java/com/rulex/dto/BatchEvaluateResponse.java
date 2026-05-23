package com.rulex.dto;

import java.util.List;

public record BatchEvaluateResponse(List<BatchResult> results, String requestId) {

    public record BatchResult(int index, String rule, Boolean result, String error, boolean success) {

        public static BatchResult ok(int index, String rule, boolean result) {
            return new BatchResult(index, rule, result, null, true);
        }

        public static BatchResult fail(int index, String rule, String error) {
            return new BatchResult(index, rule, null, error, false);
        }
    }
}
