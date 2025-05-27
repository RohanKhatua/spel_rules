package com.rules.service.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rules.service.model.Rule;
import com.rules.service.repository.RuleRepository;

@Service
public class RuleExecutionService {

    private final RuleRepository ruleRepository;
    private final ExpressionParser parser;
    private static final Logger logger = LoggerFactory.getLogger(RuleExecutionService.class);

    public RuleExecutionService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
        this.parser = new SpelExpressionParser();
    }

    /**
     * Mutable wrapper to make Map properties accessible as object properties in
     * SpEL
     * This allows access to both input data and output variables from previous
     * rules
     * Enhanced to support nested object access with dot notation
     */
    public static class PropertyAccessWrapper {
        private final Map<String, Object> properties;
        private final Map<String, Object> outputVariables;

        public PropertyAccessWrapper(Map<String, Object> properties) {
            this.properties = new HashMap<>(properties);
            this.outputVariables = new HashMap<>();
        }

        // Add output variable for direct access in subsequent rules
        public void addOutputVariable(String name, Object value) {
            outputVariables.put(name, value);
        }

        // This method allows SpEL to access any property dynamically with support for
        // nested paths
        public Object get(String propertyName) {
            // Check output variables first (from previous rules), then input properties
            Object value = getNestedValue(outputVariables, propertyName);
            if (value != null) {
                return value;
            }
            return getNestedValue(properties, propertyName);
        }

        /**
         * Get value from nested object structure using dot notation
         * Enhanced to support array index access (e.g., "users[0].name",
         * "company.employees[1].profile.title")
         * 
         * @param source The source map to search in
         * @param path   The property path (e.g., "user.profile.name", "users[0].name")
         * @return The value at the nested path, or null if not found
         */
        private Object getNestedValue(Map<String, Object> source, String path) {
            if (path == null || path.isEmpty()) {
                return null;
            }

            String[] parts = path.split("\\.");
            Object current = source;

            for (String part : parts) {
                if (current == null) {
                    return null;
                }

                // Check if this part contains array index notation like "users[0]"
                if (part.contains("[") && part.contains("]")) {
                    current = handleArrayAccess(current, part);
                } else {
                    // Regular property access
                    if (current instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> currentMap = (Map<String, Object>) current;
                        current = currentMap.get(part);
                    } else {
                        // If current is not a Map, we can't navigate further
                        return null;
                    }
                }
            }

            return current;
        }

        /**
         * Handle array index access like "users[0]" or "employees[1]"
         * 
         * @param current The current object (should be a Map)
         * @param part    The part containing array access notation
         * @return The object at the specified array index, or null if not found/invalid
         */
        private Object handleArrayAccess(Object current, String part) {
            if (!(current instanceof Map)) {
                return null;
            }

            try {
                // Parse property name and index from "propertyName[index]"
                int openBracket = part.indexOf('[');
                int closeBracket = part.indexOf(']');

                if (openBracket == -1 || closeBracket == -1 || openBracket >= closeBracket) {
                    return null;
                }

                String propertyName = part.substring(0, openBracket);
                String indexStr = part.substring(openBracket + 1, closeBracket);

                int index = Integer.parseInt(indexStr);

                @SuppressWarnings("unchecked")
                Map<String, Object> currentMap = (Map<String, Object>) current;
                Object arrayProperty = currentMap.get(propertyName);

                if (arrayProperty == null) {
                    return null;
                }

                // Handle different array/list types
                if (arrayProperty instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> list = (java.util.List<Object>) arrayProperty;
                    if (index >= 0 && index < list.size()) {
                        return list.get(index);
                    }
                } else if (arrayProperty.getClass().isArray()) {
                    Object[] array = (Object[]) arrayProperty;
                    if (index >= 0 && index < array.length) {
                        return array[index];
                    }
                }

                return null;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                // Invalid index format or parsing error
                return null;
            }
        }

        // Common property accessors for direct access
        public Object getAge() {
            return get("age");
        }

        public Object getName() {
            return get("name");
        }

        public Object getEmail() {
            return get("email");
        }

        public Object getId() {
            return get("id");
        }

        public Object getStatus() {
            return get("status");
        }

        // Generic property access using reflection-like approach
        public Object getProperty(String name) {
            return get(name);
        }

        // Allow accessing any property dynamically (this enables direct property access
        // in SpEL)
        public Object getValue(String propertyName) {
            return get(propertyName);
        }

        // Get available output variables (for debugging/logging)
        public Map<String, Object> getOutputVariables() {
            return new HashMap<>(outputVariables);
        }

        // ===== FUNCTION METHODS =====
        // These allow calling functions directly without # prefix

        /**
         * Convert string to uppercase
         */
        public String STRING_UPPERCASE(String input) {
            return input != null ? input.toUpperCase() : null;
        }

        /**
         * Convert string to lowercase
         */
        public String STRING_LOWERCASE(String input) {
            return input != null ? input.toLowerCase() : null;
        }

        /**
         * Get substring
         */
        public String STRING_SUBSTRING(String input, int start, int end) {
            if (input == null)
                return null;
            try {
                return input.substring(start, end);
            } catch (StringIndexOutOfBoundsException e) {
                return input;
            }
        }

        /**
         * String concatenation - supports variable number of arguments
         */
        public String STRING_CONCAT(String... strings) {
            if (strings == null || strings.length == 0) {
                return "";
            }
            StringBuilder result = new StringBuilder();
            for (String str : strings) {
                result.append(str != null ? str : "");
            }
            return result.toString();
        }

        @Override
        public String toString() {
            Map<String, Object> allProperties = new HashMap<>(properties);
            allProperties.putAll(outputVariables);
            return allProperties.toString();
        }
    }

    /**
     * Custom PropertyAccessor to enable dynamic property access on
     * PropertyAccessWrapper
     */
    public static class PropertyAccessWrapperAccessor implements PropertyAccessor {
        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class<?>[] { PropertyAccessWrapper.class };
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
            return target instanceof PropertyAccessWrapper;
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
            if (target instanceof PropertyAccessWrapper) {
                PropertyAccessWrapper wrapper = (PropertyAccessWrapper) target;
                Object value = wrapper.get(name);
                return new TypedValue(value);
            }
            throw new AccessException("Cannot read property '" + name + "' from " + target.getClass());
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
            return false; // We don't support writing
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue)
                throws AccessException {
            throw new AccessException("Writing not supported");
        }
    }

    /**
     * Custom PropertyAccessor to handle nested Map access directly
     * This allows SpEL to navigate nested Map structures using dot notation
     */
    public static class NestedMapPropertyAccessor implements PropertyAccessor {
        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class<?>[] { Map.class };
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
            return target instanceof Map;
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
            if (target instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) target;
                Object value = map.get(name);
                return new TypedValue(value);
            }
            throw new AccessException("Cannot read property '" + name + "' from " + target.getClass());
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
            return false; // We don't support writing
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue)
                throws AccessException {
            throw new AccessException("Writing not supported");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> executeRuleset(String rulesetName, Map<String, Object> inputData) {
        return executeRuleset(rulesetName, inputData, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> executeRuleset(String rulesetName, Map<String, Object> inputData,
            boolean nullSafeEvaluation) {
        List<Rule> rules = ruleRepository.findByRuleset(rulesetName);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("Ruleset not found: " + rulesetName);
        }

        Map<String, Object> outputVariables = new HashMap<>();

        // Create wrapper for direct property access
        PropertyAccessWrapper rootObject = new PropertyAccessWrapper(inputData);
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);

        // Register custom property accessors for dynamic property access
        context.addPropertyAccessor(new PropertyAccessWrapperAccessor());
        context.addPropertyAccessor(new NestedMapPropertyAccessor());

        try {
            // Register string functions
            context.registerFunction("STRING_UPPERCASE",
                    RuleExecutionService.class.getDeclaredMethod("toUpperCase", String.class));
        } catch (NoSuchMethodException e) {
            logger.error("Failed to register string functions: {}", e.getMessage());
            throw new RuntimeException("Failed to register string functions", e);
        }

        // Also add input data as variables for backward compatibility with # syntax
        inputData.forEach(context::setVariable);

        // Debug: Test accessing variables
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

        // Execute rules in order
        for (Rule rule : rules) {
            logger.info("Executing rule {}: condition='{}', transformation='{}'",
                    rule.getId(), rule.getCondition(), rule.getTransformation());
            logger.debug("Available output variables from previous rules: {}",
                    rootObject.getOutputVariables().keySet());
            try {
                // Parse and evaluate condition
                Expression conditionExpr = parser.parseExpression(rule.getCondition());
                Boolean conditionResult = safeEvaluateCondition(conditionExpr, context, rule, nullSafeEvaluation);

                logger.info("Rule {} condition result: {}", rule.getId(), conditionResult);

                if (Boolean.TRUE.equals(conditionResult)) {
                    // Parse and evaluate transformation
                    Expression transformExpr = parser.parseExpression(rule.getTransformation());
                    Object result = safeEvaluateTransformation(transformExpr, context, rule, nullSafeEvaluation);

                    // Store result in output variable
                    outputVariables.put(rule.getOutputVariable(), result);
                    // Add to context for # syntax access
                    context.setVariable(rule.getOutputVariable(), result);
                    // Add to root object for direct property access
                    rootObject.addOutputVariable(rule.getOutputVariable(), result);

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

        return outputVariables;
    }

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

    private boolean isNullPropertyAccess(SpelEvaluationException e) {
        String message = e.getMessage();
        return message != null &&
                (message.contains("cannot be found on null") ||
                        message.contains("EL1007E") ||
                        message.contains("Property or field") && message.contains("null"));
    }

    // Helper method for string functions
    public static String toUpperCase(String input) {
        return input != null ? input.toUpperCase() : null;
    }
}