package com.rules.service.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rules.service.model.Rule;

/**
 * Service responsible for executing rule sets against input data
 */
@Service
public class RuleExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(RuleExecutionService.class);

    private final RuleService ruleService;
    private final ExpressionParser parser;
    private final SpelContextConfigurationService spelContextService;

    public RuleExecutionService(RuleService ruleService, SpelContextConfigurationService spelContextService) {
        this.ruleService = ruleService;
        this.parser = new SpelExpressionParser();
        this.spelContextService = spelContextService;
    }

    /**
     * Execute a ruleset with null-safe evaluation enabled by default
     */
    @Transactional(readOnly = true)
    public Map<String, Object> executeRuleset(String rulesetName, Map<String, Object> inputData) {
        return executeRuleset(rulesetName, inputData, true);
    }

    /**
     * Execute a ruleset against input data
     * 
     * @param rulesetName        The name of the ruleset to execute
     * @param inputData          The input data for rule evaluation
     * @param nullSafeEvaluation Whether to handle null property access gracefully
     * @return Map of output variables from rule execution
     */
    @Transactional(readOnly = true)
    public Map<String, Object> executeRuleset(String rulesetName, Map<String, Object> inputData,
            boolean nullSafeEvaluation) {

        List<Rule> rules = getRulesForRuleset(rulesetName);
        Map<String, Object> outputVariables = new HashMap<>();

        StandardEvaluationContext context = spelContextService.createEvaluationContext(inputData);
        PropertyAccessWrapper rootObject = (PropertyAccessWrapper) context.getRootObject().getValue();

        logInputDataForDebugging(inputData, context);

        // Execute rules in order
        for (Rule rule : rules) {
            executeRule(rule, context, rootObject, outputVariables, nullSafeEvaluation);
        }

        return outputVariables;
    }

    /**
     * Get rules for a specific ruleset
     */
    private List<Rule> getRulesForRuleset(String rulesetName) {
        List<Rule> rules = ruleService.getRulesByRuleset(rulesetName);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("Ruleset not found: " + rulesetName);
        }
        return rules;
    }

    /**
     * Execute a single rule
     */
    private void executeRule(Rule rule, StandardEvaluationContext context, PropertyAccessWrapper rootObject,
            Map<String, Object> outputVariables, boolean nullSafeEvaluation) {

        logger.info("Executing rule {}: condition='{}', transformation='{}'",
                rule.getId(), rule.getCondition(), rule.getTransformation());
        logger.debug("Available output variables from previous rules: {}",
                rootObject.getOutputVariables().keySet());

        try {
            if (evaluateCondition(rule, context, nullSafeEvaluation)) {
                Object result = evaluateTransformation(rule, context, nullSafeEvaluation);
                storeRuleResult(rule, result, context, rootObject, outputVariables);
                logger.info("Rule {} executed successfully. Output: {} = {}",
                        rule.getId(), rule.getOutputVariable(), result);
            } else {
                logger.info("Rule {} condition was false, skipping transformation", rule.getId());
            }
        } catch (ExpressionException e) {
            logger.error("Error executing rule {}: condition='{}', transformation='{}', error='{}'",
                    rule.getId(), rule.getCondition(), rule.getTransformation(), e.getMessage());
            throw new RuntimeException("Error executing rule " + rule.getId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Evaluate rule condition
     */
    private boolean evaluateCondition(Rule rule, StandardEvaluationContext context, boolean nullSafeEvaluation) {
        Expression conditionExpr = parser.parseExpression(rule.getCondition());
        Boolean conditionResult = safeEvaluateCondition(conditionExpr, context, rule, nullSafeEvaluation);
        logger.info("Rule {} condition result: {}", rule.getId(), conditionResult);
        return Boolean.TRUE.equals(conditionResult);
    }

    /**
     * Evaluate rule transformation
     */
    private Object evaluateTransformation(Rule rule, StandardEvaluationContext context, boolean nullSafeEvaluation) {
        Expression transformExpr = parser.parseExpression(rule.getTransformation());
        return safeEvaluateTransformation(transformExpr, context, rule, nullSafeEvaluation);
    }

    /**
     * Store rule execution result
     */
    private void storeRuleResult(Rule rule, Object result, StandardEvaluationContext context,
            PropertyAccessWrapper rootObject, Map<String, Object> outputVariables) {
        outputVariables.put(rule.getOutputVariable(), result);
        spelContextService.addOutputVariable(context, rootObject, rule.getOutputVariable(), result);
    }

    /**
     * Safely evaluate condition with null handling
     */
    private Boolean safeEvaluateCondition(Expression conditionExpr, StandardEvaluationContext context,
            Rule rule, boolean nullSafeEvaluation) {
        try {
            return conditionExpr.getValue(context, Boolean.class);
        } catch (SpelEvaluationException e) {
            if (nullSafeEvaluation && isNullPropertyAccess(e)) {
                logger.warn("Null property access in rule {} condition '{}'. Treating as false. Error: {}",
                        rule.getId(), rule.getCondition(), e.getMessage());
                return false;
            }
            throw e;
        }
    }

    /**
     * Safely evaluate transformation with null handling
     */
    private Object safeEvaluateTransformation(Expression transformExpr, StandardEvaluationContext context,
            Rule rule, boolean nullSafeEvaluation) {
        try {
            return transformExpr.getValue(context);
        } catch (SpelEvaluationException e) {
            if (nullSafeEvaluation && isNullPropertyAccess(e)) {
                logger.warn("Null property access in rule {} transformation '{}'. Returning null. Error: {}",
                        rule.getId(), rule.getTransformation(), e.getMessage());
                return null;
            }
            throw e;
        }
    }

    /**
     * Check if exception is due to null property access
     */
    private boolean isNullPropertyAccess(SpelEvaluationException e) {
        String message = e.getMessage();
        return message != null &&
                (message.contains("cannot be found on null") ||
                        message.contains("EL1007E") ||
                        message.contains("Property or field") && message.contains("null"));
    }

    /**
     * Log input data for debugging purposes
     */
    private void logInputDataForDebugging(Map<String, Object> inputData, StandardEvaluationContext context) {
        logger.info("Input data: {}", inputData);
        try {
            // Test direct property access (new way)
            Expression testDirectExpr = parser.parseExpression("age");
            Object testDirectResult = testDirectExpr.getValue(context);
            logger.info("Test direct 'age' expression result: {}", testDirectResult);

            // Test variable access (old way)
            Expression testVarExpr = parser.parseExpression("#age");
            Object testVarResult = testVarExpr.getValue(context);
            logger.info("Test '#age' expression result: {}", testVarResult);
        } catch (ExpressionException e) {
            logger.warn("Error testing age access: {}", e.getMessage());
        }
    }
}