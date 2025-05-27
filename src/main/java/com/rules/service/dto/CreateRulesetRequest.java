package com.rules.service.dto;

import java.util.List;

public class CreateRulesetRequest {
    private String name;
    private List<RuleRequest> rules;

    public CreateRulesetRequest() {
    }

    public CreateRulesetRequest(String name, List<RuleRequest> rules) {
        this.name = name;
        this.rules = rules;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RuleRequest> getRules() {
        return rules;
    }

    public void setRules(List<RuleRequest> rules) {
        this.rules = rules;
    }
}
