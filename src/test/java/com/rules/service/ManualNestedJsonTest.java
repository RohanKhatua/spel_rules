package com.rules.service;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rules.service.model.Rule;
import com.rules.service.repository.RuleRepository;
import com.rules.service.service.RuleExecutionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Manual Nested JSON Test")
public class ManualNestedJsonTest {

    @Mock
    private RuleRepository ruleRepository;

    @InjectMocks
    private RuleExecutionService ruleExecutionService;

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
        when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

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