package com.rules.service.service;

import org.springframework.stereotype.Service;

@Service
public class RuleParserService {

    public RuleParts parseRule(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule cannot be empty");
        }

        String[] parts = rule.split("THEN");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Rule must contain 'THEN' keyword");
        }

        String condition = parts[0].trim();
        String transformation = parts[1].trim();

        return new RuleParts(condition, transformation);
    }

    public static class RuleParts {
        private final String condition;
        private final String transformation;

        public RuleParts(String condition, String transformation) {
            this.condition = condition;
            this.transformation = transformation;
        }

        public String getCondition() {
            return condition;
        }

        public String getTransformation() {
            return transformation;
        }
    }
}