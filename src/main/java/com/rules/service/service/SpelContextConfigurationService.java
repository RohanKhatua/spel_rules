package com.rules.service.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

/**
 * Service responsible for configuring SpEL evaluation contexts
 */
@Service
public class SpelContextConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(SpelContextConfigurationService.class);

    /**
     * Create and configure a StandardEvaluationContext for rule evaluation
     * 
     * @param inputData Input data for the rule evaluation
     * @return Configured StandardEvaluationContext
     */
    public StandardEvaluationContext createEvaluationContext(Map<String, Object> inputData) {
        // Create wrapper for direct property access
        PropertyAccessWrapper rootObject = new PropertyAccessWrapper(inputData);
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);

        // Register custom property accessors for dynamic property access
        context.addPropertyAccessor(new PropertyAccessWrapperAccessor());
        context.addPropertyAccessor(new NestedMapPropertyAccessor());

        // Register SpEL functions
        registerSpelFunctions(context);

        // Add input data as variables for backward compatibility with # syntax
        inputData.forEach(context::setVariable);

        return context;
    }

    /**
     * Register SpEL functions in the evaluation context
     * 
     * @param context The evaluation context to register functions in
     */
    private void registerSpelFunctions(StandardEvaluationContext context) {
        try {
            // Register string functions
            context.registerFunction("STRING_UPPERCASE",
                    SpelFunctionUtils.class.getDeclaredMethod("toUpperCase", String.class));
            context.registerFunction("STRING_LOWERCASE",
                    SpelFunctionUtils.class.getDeclaredMethod("toLowerCase", String.class));
            context.registerFunction("STRING_SUBSTRING",
                    SpelFunctionUtils.class.getDeclaredMethod("substring", String.class, int.class, int.class));
            context.registerFunction("STRING_CONCAT",
                    SpelFunctionUtils.class.getDeclaredMethod("concat", String[].class));
            context.registerFunction("STRING_LENGTH",
                    SpelFunctionUtils.class.getDeclaredMethod("length", String.class));
            context.registerFunction("STRING_CONTAINS",
                    SpelFunctionUtils.class.getDeclaredMethod("contains", String.class, String.class));
            context.registerFunction("STRING_STARTS_WITH",
                    SpelFunctionUtils.class.getDeclaredMethod("startsWith", String.class, String.class));
            context.registerFunction("STRING_ENDS_WITH",
                    SpelFunctionUtils.class.getDeclaredMethod("endsWith", String.class, String.class));
            context.registerFunction("STRING_TRIM",
                    SpelFunctionUtils.class.getDeclaredMethod("trim", String.class));

            logger.debug("Successfully registered SpEL functions");
        } catch (NoSuchMethodException e) {
            logger.error("Failed to register SpEL functions: {}", e.getMessage());
            throw new RuntimeException("Failed to register SpEL functions", e);
        }
    }

    /**
     * Update the evaluation context with new output variables
     * 
     * @param context      The evaluation context to update
     * @param rootObject   The root object wrapper
     * @param variableName The name of the output variable
     * @param value        The value of the output variable
     */
    public void addOutputVariable(StandardEvaluationContext context, PropertyAccessWrapper rootObject,
            String variableName, Object value) {
        // Add to context for # syntax access
        context.setVariable(variableName, value);
        // Add to root object for direct property access
        rootObject.addOutputVariable(variableName, value);
    }
}