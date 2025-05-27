package com.rules.service;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.rules.service.model.Rule;
import com.rules.service.service.NestedMapPropertyAccessor;
import com.rules.service.service.PropertyAccessWrapper;
import com.rules.service.service.PropertyAccessWrapperAccessor;
import com.rules.service.service.RuleExecutionService;
import com.rules.service.service.RuleService;
import com.rules.service.service.SpelContextConfigurationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Manual Nested JSON Test")
public class ManualNestedJsonTest {

    @Mock
    private RuleService ruleService;

    @Mock
    private SpelContextConfigurationService spelContextConfigurationService;

    @InjectMocks
    private RuleExecutionService ruleExecutionService;

    @BeforeEach
    void setUp() {
        // Mock the SpelContextConfigurationService to create a proper context with
        // input data
        when(spelContextConfigurationService.createEvaluationContext(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> inputData = (Map<String, Object>) invocation.getArgument(0);
            PropertyAccessWrapper wrapper = new PropertyAccessWrapper(inputData);
            StandardEvaluationContext context = new StandardEvaluationContext(wrapper);
            context.addPropertyAccessor(new PropertyAccessWrapperAccessor());
            context.addPropertyAccessor(new NestedMapPropertyAccessor());

            // Add input data as variables for backward compatibility
            inputData.forEach(context::setVariable);

            return context;
        });
    }

    private Rule createRule(String condition, String transformation, String outputVariable) {
        Rule rule = new Rule();
        rule.setCondition(condition);
        rule.setTransformation(transformation);
        rule.setOutputVariable(outputVariable);
        rule.setRuleset("test_ruleset");
        return rule;
    }

    @Test
    @DisplayName("Verify nested JSON object access with dot notation works")
    void testNestedJsonAccess() {
        // Arrange
        Rule rule = createRule("user.profile.age >= 25",
                "STRING_CONCAT(\"Hello \", user.profile.name)", "greeting");
        when(ruleService.getRulesByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

        // Create nested JSON structure
        Map<String, Object> profile = Map.of(
                "name", "Alice",
                "age", 30);
        Map<String, Object> user = Map.of("profile", profile);
        Map<String, Object> inputData = Map.of("user", user);

        // Act
        Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

        // Assert
        assertThat(result).containsEntry("greeting", "Hello Alice");
    }
}