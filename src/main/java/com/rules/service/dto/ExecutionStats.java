package com.rules.service.dto;

public class ExecutionStats {
    private int totalRules;
    private int executedRules;

    public ExecutionStats() {
    }

    public ExecutionStats(int totalRules, int executedRules) {
        this.totalRules = totalRules;
        this.executedRules = executedRules;
    }

    public int getTotalRules() {
        return totalRules;
    }

    public void setTotalRules(int totalRules) {
        this.totalRules = totalRules;
    }

    public int getExecutedRules() {
        return executedRules;
    }

    public void setExecutedRules(int executedRules) {
        this.executedRules = executedRules;
    }
}