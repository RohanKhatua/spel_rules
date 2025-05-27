package com.rules.service.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rules.service.model.Rule;
import com.rules.service.repository.RuleRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RuleExecutionService Unit Tests")
class RuleExecutionServiceTest {

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

    @Nested
    @DisplayName("Basic Transformation Tests")
    class BasicTransformationTests {

        @Test
        @DisplayName("STRING_UPPERCASE transformation with direct property access")
        void testStringUppercaseWithDirectAccess() {
            // Arrange
            Rule rule = createRule("age >= 18", "STRING_UPPERCASE(name)", "name_upper");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("name", "alice");
            inputData.put("age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("name_upper", "ALICE");
        }

        @Test
        @DisplayName("STRING_LOWERCASE transformation")
        void testStringLowercase() {
            // Arrange
            Rule rule = createRule("age >= 18", "STRING_LOWERCASE(name)", "name_lower");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "JOHN", "age", 30);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("name_lower", "john");
        }

        @Test
        @DisplayName("STRING_CONCAT transformation")
        void testStringConcat() {
            // Arrange
            Rule rule = createRule("age >= 18", "STRING_CONCAT(\"Hello, \", name)", "greeting");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "world", "age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("greeting", "Hello, world");
        }

        @Test
        @DisplayName("STRING_SUBSTRING transformation")
        void testStringSubstring() {
            // Arrange
            Rule rule = createRule("name.length() > 5", "STRING_SUBSTRING(name, 0, 3)", "name_short");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "Alexander", "age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("name_short", "Ale");
        }
    }

    @Nested
    @DisplayName("Nested Transformation Tests")
    class NestedTransformationTests {

        @Test
        @DisplayName("STRING_UPPERCASE(STRING_CONCAT()) nested transformation")
        void testNestedUppercaseConcat() {
            // Arrange
            Rule rule = createRule("age >= 18", "STRING_UPPERCASE(STRING_CONCAT(\"Mr. \", name))", "formal_name");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "smith", "age", 30);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("formal_name", "MR. SMITH");
        }

        @Test
        @DisplayName("Complex nested transformation for proper case")
        void testComplexNestedTransformation() {
            // Arrange
            String transformation = "STRING_CONCAT(STRING_UPPERCASE(STRING_SUBSTRING(name, 0, 1)), STRING_LOWERCASE(STRING_SUBSTRING(name, 1, name.length())))";
            Rule rule = createRule("name.length() > 3", transformation, "proper_case");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "jOHN", "age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("proper_case", "John");
        }

        @Test
        @DisplayName("Triple nested transformation")
        void testTripleNestedTransformation() {
            // Arrange
            Rule rule = createRule("age >= 21", "STRING_UPPERCASE(STRING_CONCAT(\"DR. \", STRING_LOWERCASE(name)))",
                    "doctor_title");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "WATSON", "age", 35);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("doctor_title", "DR. WATSON");
        }

        @Test
        @DisplayName("Quad nested transformation")
        void testQuadNestedTransformation() {
            // Arrange
            String transformation = "STRING_UPPERCASE(STRING_CONCAT(STRING_CONCAT(\"Prof. \", STRING_LOWERCASE(name)), STRING_CONCAT(\" - \", age.toString())))";
            Rule rule = createRule("age >= 30", transformation, "professor_title");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "EINSTEIN", "age", 45);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("professor_title", "PROF. EINSTEIN - 45");
        }
    }

    @Nested
    @DisplayName("Complex Condition Tests")
    class ComplexConditionTests {

        @Test
        @DisplayName("Multiple AND conditions")
        void testMultipleAndConditions() {
            // Arrange
            Rule rule = createRule("age >= 18 AND name.length() > 3 AND age < 65",
                    "STRING_CONCAT(name, \" - Valid Adult\")", "status");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            // Test valid case
            Map<String, Object> inputData = Map.of("name", "Alice", "age", 25);
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);
            assertThat(result).containsEntry("status", "Alice - Valid Adult");

            // Test invalid case - too young
            inputData = Map.of("name", "Bob", "age", 16);
            result = ruleExecutionService.executeRuleset("test_ruleset", inputData);
            assertThat(result).doesNotContainKey("status");

            // Test invalid case - name too short
            inputData = Map.of("name", "Jo", "age", 25);
            result = ruleExecutionService.executeRuleset("test_ruleset", inputData);
            assertThat(result).doesNotContainKey("status");
        }

        @Test
        @DisplayName("OR conditions")
        void testOrConditions() {
            // Arrange
            Rule rule = createRule("age >= 65 OR age <= 12", "STRING_CONCAT(name, \" - Special Rate\")", "rate");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            // Test senior citizen
            Map<String, Object> inputData = Map.of("name", "George", "age", 70);
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);
            assertThat(result).containsEntry("rate", "George - Special Rate");

            // Test child
            inputData = Map.of("name", "Emma", "age", 8);
            result = ruleExecutionService.executeRuleset("test_ruleset", inputData);
            assertThat(result).containsEntry("rate", "Emma - Special Rate");

            // Test regular adult
            inputData = Map.of("name", "John", "age", 35);
            result = ruleExecutionService.executeRuleset("test_ruleset", inputData);
            assertThat(result).doesNotContainKey("rate");
        }
    }

    @Nested
    @DisplayName("Built-in Method Tests")
    class BuiltInMethodTests {

        @Test
        @DisplayName("Native string methods work correctly")
        void testNativeStringMethods() {
            // Arrange
            Rule rule = createRule("name.length() > 5", "name.toUpperCase()", "name_upper");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "alexander", "age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("name_upper", "ALEXANDER");
        }

        @Test
        @DisplayName("Combining native methods with custom functions")
        void testMixedMethods() {
            // Arrange
            String transformation = "STRING_CONCAT(name.substring(0, 1).toUpperCase(), name.substring(1).toLowerCase())";
            Rule rule = createRule("name.length() > 3", transformation, "title_case");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "mARY", "age", 28);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("title_case", "Mary");
        }
    }

    @Nested
    @DisplayName("Multiple Rules Tests")
    class MultipleRulesTests {

        @Test
        @DisplayName("Multiple rules execute in sequence")
        void testMultipleRulesInSequence() {
            // Arrange
            Rule rule1 = createRule("age >= 18", "\"ADULT\"", "age_category");
            Rule rule2 = createRule("age >= 18", "STRING_UPPERCASE(name)", "name_upper");
            Rule rule3 = createRule("age >= 21", "STRING_CONCAT(name, \" can drink\")", "drink_status");

            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule1, rule2, rule3));

            Map<String, Object> inputData = Map.of("name", "john", "age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).containsEntry("age_category", "ADULT");
            assertThat(result).containsEntry("name_upper", "JOHN");
            assertThat(result).containsEntry("drink_status", "john can drink");
        }

        @Test
        @DisplayName("Rules can use output from previous rules")
        void testRulesUsingPreviousOutputs() {
            // Arrange
            Rule rule1 = createRule("age >= 18", "STRING_UPPERCASE(name)", "name_upper");
            Rule rule2 = createRule("age >= 21", "STRING_CONCAT(\"Mr. \", name_upper)", "formal_greeting");

            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule1, rule2));

            Map<String, Object> inputData = Map.of("name", "smith", "age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("name_upper", "SMITH");
            assertThat(result).containsEntry("formal_greeting", "Mr. SMITH");
        }

        @Test
        @DisplayName("Output variables can be accessed both with and without # prefix")
        void testOutputVariableAccessMethods() {
            // Arrange
            Rule rule1 = createRule("age >= 18", "STRING_UPPERCASE(name)", "name_upper");
            Rule rule2 = createRule("age >= 21", "STRING_CONCAT(\"Direct: \", name_upper)", "direct_access");
            Rule rule3 = createRule("age >= 21", "STRING_CONCAT(\"Variable: \", #name_upper)", "variable_access");

            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule1, rule2, rule3));

            Map<String, Object> inputData = Map.of("name", "alice", "age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("name_upper", "ALICE");
            assertThat(result).containsEntry("direct_access", "Direct: ALICE");
            assertThat(result).containsEntry("variable_access", "Variable: ALICE");
        }

        @Test
        @DisplayName("Complex rule chain with multiple output variables")
        void testComplexRuleChain() {
            // Arrange
            Rule rule1 = createRule("age >= 18", "STRING_UPPERCASE(name)", "name_upper");
            Rule rule2 = createRule("age >= 21", "age + 10", "future_age");
            Rule rule3 = createRule("name_upper != null",
                    "STRING_CONCAT(name_upper, \" will be \", future_age.toString(), \" in 10 years\")", "prediction");

            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule1, rule2, rule3));

            Map<String, Object> inputData = Map.of("name", "bob", "age", 30);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).containsEntry("name_upper", "BOB");
            assertThat(result).containsEntry("future_age", 40);
            assertThat(result).containsEntry("prediction", "BOB will be 40 in 10 years");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Null values are handled gracefully")
        void testNullHandling() {
            // Arrange
            Rule rule = createRule("age >= 18", "STRING_UPPERCASE(name)", "name_upper");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("name", null);
            inputData.put("age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("name_upper", null);
        }

        @Test
        @DisplayName("Empty strings are handled correctly")
        void testEmptyStringHandling() {
            // Arrange
            Rule rule = createRule("age >= 18", "STRING_CONCAT(\"Hello, \", name)", "greeting");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "", "age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("greeting", "Hello, ");
        }

        @Test
        @DisplayName("False conditions don't execute transformations")
        void testFalseConditions() {
            // Arrange
            Rule rule = createRule("age >= 21", "STRING_UPPERCASE(name)", "name_upper");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "john", "age", 18);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Non-existent ruleset throws exception")
        void testNonExistentRuleset() {
            // Arrange
            when(ruleRepository.findByRuleset("non_existent")).thenReturn(Arrays.asList());

            Map<String, Object> inputData = Map.of("name", "john", "age", 25);

            // Act & Assert
            assertThatThrownBy(() -> ruleExecutionService.executeRuleset("non_existent", inputData))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Ruleset not found: non_existent");
        }

        @Test
        @DisplayName("Substring with out of bounds returns original string")
        void testSubstringOutOfBounds() {
            // Arrange
            Rule rule = createRule("true", "STRING_SUBSTRING(name, 0, 100)", "name_sub");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "short", "age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("name_sub", "short");
        }
    }

    @Nested
    @DisplayName("Data Type Tests")
    class DataTypeTests {

        @Test
        @DisplayName("Boolean values work in conditions")
        void testBooleanValues() {
            // Arrange
            Rule rule = createRule("isActive == true AND age >= 18", "STRING_CONCAT(name, \" - Active\")", "status");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "Bob", "age", 30, "isActive", true);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("status", "Bob - Active");
        }

        @Test
        @DisplayName("Numeric values work in transformations")
        void testNumericValues() {
            // Arrange
            Rule rule = createRule("age >= 18", "STRING_CONCAT(name, \" is \", age.toString(), \" years old\")",
                    "description");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> inputData = Map.of("name", "Alice", "age", 25);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("description", "Alice is 25 years old");
        }
    }
}