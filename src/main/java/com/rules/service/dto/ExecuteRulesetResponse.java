package com.rules.service.dto;

import java.util.Map;

public class ExecuteRulesetResponse {
    private Map<String, Object> outputVariables;
    private ExecutionStats stats;

    public ExecuteRulesetResponse() {
    }

    public ExecuteRulesetResponse(Map<String, Object> outputVariables, ExecutionStats stats) {
        this.outputVariables = outputVariables;
        this.stats = stats;
    }

    public Map<String, Object> getOutputVariables() {
        return outputVariables;
    }

    public void setOutputVariables(Map<String, Object> outputVariables) {
        this.outputVariables = outputVariables;
    }

    public ExecutionStats getStats() {
        return stats;
    }

    public void setStats(ExecutionStats stats) {
        this.stats = stats;
    }
}