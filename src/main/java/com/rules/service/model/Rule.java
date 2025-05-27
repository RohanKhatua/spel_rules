package com.rules.service.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Rule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String condition;

    @Column(nullable = false)
    private String transformation;

    @Column(nullable = false)
    private String outputVariable;

    @Column(nullable = false)
    private String ruleset;

    public Rule() {
    }

    public Rule(String condition, String transformation, String outputVariable, String ruleset) {
        this.condition = condition;
        this.transformation = transformation;
        this.outputVariable = outputVariable;
        this.ruleset = ruleset;
    }

    public UUID getId() {
        return id;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    public String getOutputVariable() {
        return outputVariable;
    }

    public void setOutputVariable(String outputVariable) {
        this.outputVariable = outputVariable;
    }

    public String getRuleset() {
        return ruleset;
    }

    public void setRuleset(String ruleset) {
        this.ruleset = ruleset;
    }
}