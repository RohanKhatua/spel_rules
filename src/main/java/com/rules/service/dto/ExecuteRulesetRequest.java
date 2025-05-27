package com.rules.service.dto;

import java.util.Map;

public class ExecuteRulesetRequest {
    private String rulesetName;
    private Map<String, Object> inputData;

    public ExecuteRulesetRequest() {
    }

    public ExecuteRulesetRequest(String rulesetName, Map<String, Object> inputData) {
        this.rulesetName = rulesetName;
        this.inputData = inputData;
    }

    public String getRulesetName() {
        return rulesetName;
    }

    public void setRulesetName(String rulesetName) {
        this.rulesetName = rulesetName;
    }

    public Map<String, Object> getInputData() {
        return inputData;
    }

    public void setInputData(Map<String, Object> inputData) {
        this.inputData = inputData;
    }
}