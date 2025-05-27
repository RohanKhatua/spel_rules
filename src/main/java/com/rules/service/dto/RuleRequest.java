package com.rules.service.dto;

public class RuleRequest {
    private String rule;
    private String outputVariable;

    public RuleRequest() {
    }

    public RuleRequest(String rule, String outputVariable) {
        this.rule = rule;
        this.outputVariable = outputVariable;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getOutputVariable() {
        return outputVariable;
    }

    public void setOutputVariable(String outputVariable) {
        this.outputVariable = outputVariable;
    }
}