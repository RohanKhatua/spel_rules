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

    @Nested
    @DisplayName("Nested JSON Object Access Tests")
    class NestedJsonObjectTests {

        @Test
        @DisplayName("Basic nested object access with dot notation")
        void testBasicNestedObjectAccess() {
            // Arrange
            Rule rule = createRule("user.age >= 18", "STRING_UPPERCASE(user.name)", "user_name_upper");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> userData = Map.of(
                    "name", "alice",
                    "age", 25);
            Map<String, Object> inputData = Map.of("user", userData);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("user_name_upper", "ALICE");
        }

        @Test
        @DisplayName("Deep nested object access")
        void testDeepNestedObjectAccess() {
            // Arrange
            Rule rule = createRule("user.profile.age >= 21",
                    "STRING_CONCAT(user.profile.firstName, \" \", user.profile.lastName)", "full_name");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> profile = Map.of(
                    "firstName", "John",
                    "lastName", "Doe",
                    "age", 30);
            Map<String, Object> user = Map.of("profile", profile);
            Map<String, Object> inputData = Map.of("user", user);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("full_name", "John Doe");
        }

        @Test
        @DisplayName("Multiple nested objects in same rule")
        void testMultipleNestedObjects() {
            // Arrange
            Rule rule = createRule("user.age >= 18 AND address.country == \"USA\"",
                    "STRING_CONCAT(user.name, \" from \", address.city)", "user_location");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> user = Map.of("name", "Bob", "age", 25);
            Map<String, Object> address = Map.of("city", "New York", "country", "USA");
            Map<String, Object> inputData = Map.of("user", user, "address", address);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("user_location", "Bob from New York");
        }

        @Test
        @DisplayName("Nested object access with null safety")
        void testNestedObjectNullSafety() {
            // Arrange
            Rule rule = createRule("user.profile.age >= 18", "STRING_UPPERCASE(user.profile.name)",
                    "profile_name_upper");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> user = Map.of("name", "alice"); // No profile object
            Map<String, Object> inputData = Map.of("user", user);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert - should handle gracefully with null safety
            assertThat(result).doesNotContainKey("profile_name_upper");
        }

        @Test
        @DisplayName("Complex nested object with arrays and mixed data types")
        void testComplexNestedStructure() {
            // Arrange
            Rule rule = createRule("company.employees.size() > 0 AND company.active == true",
                    "STRING_CONCAT(company.name, \" has \", company.employees.size().toString(), \" employees\")",
                    "company_info");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> company = Map.of(
                    "name", "TechCorp",
                    "active", true,
                    "employees", Arrays.asList("Alice", "Bob", "Charlie"));
            Map<String, Object> inputData = Map.of("company", company);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("company_info", "TechCorp has 3 employees");
        }

        @Test
        @DisplayName("Nested object access in output variables")
        void testNestedObjectInOutputVariables() {
            // Arrange
            Rule rule1 = createRule("user.age >= 18", "user.profile", "user_profile");
            Rule rule2 = createRule("user_profile != null", "STRING_UPPERCASE(user_profile.name)",
                    "profile_name_upper");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule1, rule2));

            Map<String, Object> profile = Map.of("name", "alice", "title", "engineer");
            Map<String, Object> user = Map.of("age", 25, "profile", profile);
            Map<String, Object> inputData = Map.of("user", user);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("user_profile", profile);
            assertThat(result).containsEntry("profile_name_upper", "ALICE");
        }

        @Test
        @DisplayName("Mixed flat and nested property access")
        void testMixedFlatAndNestedAccess() {
            // Arrange
            Rule rule = createRule("age >= 18 AND user.profile.verified == true",
                    "STRING_CONCAT(name, \" - \", user.profile.title)", "verified_user");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> profile = Map.of("title", "Senior Developer", "verified", true);
            Map<String, Object> user = Map.of("profile", profile);
            Map<String, Object> inputData = Map.of(
                    "name", "Alice",
                    "age", 30,
                    "user", user);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("verified_user", "Alice - Senior Developer");
        }
    }

    @Nested
    @DisplayName("Array Index Access Tests")
    class ArrayIndexAccessTests {

        @Test
        @DisplayName("Basic array index access with dot notation")
        void testBasicArrayIndexAccess() {
            // Arrange
            Rule rule = createRule("users[0].age >= 18", "STRING_UPPERCASE(users[0].name)", "first_user_name_upper");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> user1 = Map.of("name", "alice", "age", 25);
            Map<String, Object> user2 = Map.of("name", "bob", "age", 30);
            Map<String, Object> inputData = Map.of("users", Arrays.asList(user1, user2));

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("first_user_name_upper", "ALICE");
        }

        @Test
        @DisplayName("Array index access with deep nesting")
        void testArrayIndexWithDeepNesting() {
            // Arrange
            Rule rule = createRule("company.employees[1].profile.active == true",
                    "STRING_CONCAT(company.employees[1].profile.firstName, \" \", company.employees[1].profile.lastName)",
                    "second_employee_name");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> profile1 = Map.of("firstName", "Alice", "lastName", "Smith", "active", false);
            Map<String, Object> profile2 = Map.of("firstName", "Bob", "lastName", "Johnson", "active", true);
            Map<String, Object> employee1 = Map.of("profile", profile1);
            Map<String, Object> employee2 = Map.of("profile", profile2);
            Map<String, Object> company = Map.of("employees", Arrays.asList(employee1, employee2));
            Map<String, Object> inputData = Map.of("company", company);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("second_employee_name", "Bob Johnson");
        }

        @Test
        @DisplayName("Array index access with mixed property types")
        void testArrayIndexWithMixedTypes() {
            // Arrange
            Rule rule = createRule("orders[0].amount >= 100 AND orders[0].status == \"completed\"",
                    "STRING_CONCAT(\"Order #\", orders[0].id.toString(), \" - $\", orders[0].amount.toString())",
                    "order_summary");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> order1 = Map.of("id", 123, "amount", 150.50, "status", "completed");
            Map<String, Object> order2 = Map.of("id", 124, "amount", 75.25, "status", "pending");
            Map<String, Object> inputData = Map.of("orders", Arrays.asList(order1, order2));

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("order_summary", "Order #123 - $150.5");
        }

        @Test
        @DisplayName("Array index access with null safety using size checks")
        void testArrayIndexWithNullSafety() {
            // Arrange
            Rule rule = createRule("users.size() > 0 AND users[0].age >= 18", "STRING_UPPERCASE(users[0].name)",
                    "first_user_name");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            // Test with empty array - condition should be false
            Map<String, Object> inputData1 = Map.of("users", Arrays.asList());
            Map<String, Object> result1 = ruleExecutionService.executeRuleset("test_ruleset", inputData1);
            assertThat(result1).doesNotContainKey("first_user_name");

            // Test with non-empty array - should work
            Map<String, Object> user = Map.of("name", "alice", "age", 25);
            Map<String, Object> inputData2 = Map.of("users", Arrays.asList(user));
            Map<String, Object> result2 = ruleExecutionService.executeRuleset("test_ruleset", inputData2);
            assertThat(result2).containsEntry("first_user_name", "ALICE");
        }

        @Test
        @DisplayName("Multiple array index accesses in same rule")
        void testMultipleArrayIndexAccesses() {
            // Arrange
            Rule rule = createRule("users[0].age >= 18 AND users[1].age >= 21",
                    "STRING_CONCAT(users[0].name, \" and \", users[1].name, \" are both adults\")",
                    "adult_users");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> user1 = Map.of("name", "Alice", "age", 20);
            Map<String, Object> user2 = Map.of("name", "Bob", "age", 25);
            Map<String, Object> user3 = Map.of("name", "Charlie", "age", 16);
            Map<String, Object> inputData = Map.of("users", Arrays.asList(user1, user2, user3));

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("adult_users", "Alice and Bob are both adults");
        }

        @Test
        @DisplayName("Array index access combined with regular nested access")
        void testArrayIndexWithRegularNesting() {
            // Arrange
            Rule rule = createRule("company.employees[0].active == true AND company.name != null",
                    "STRING_CONCAT(company.name, \" - Employee: \", company.employees[0].profile.fullName)",
                    "company_employee_info");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> profile = Map.of("fullName", "Alice Johnson");
            Map<String, Object> employee = Map.of("active", true, "profile", profile);
            Map<String, Object> company = Map.of("name", "TechCorp", "employees", Arrays.asList(employee));
            Map<String, Object> inputData = Map.of("company", company);

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("company_employee_info", "TechCorp - Employee: Alice Johnson");
        }

        @Test
        @DisplayName("Array index access with different array positions")
        void testDifferentArrayPositions() {
            // Arrange
            Rule rule = createRule("products[2].inStock == true",
                    "STRING_CONCAT(\"Available: \", products[2].name, \" - $\", products[2].price.toString())",
                    "third_product_info");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            Map<String, Object> product1 = Map.of("name", "Laptop", "price", 999.99, "inStock", false);
            Map<String, Object> product2 = Map.of("name", "Mouse", "price", 29.99, "inStock", true);
            Map<String, Object> product3 = Map.of("name", "Keyboard", "price", 89.99, "inStock", true);
            Map<String, Object> inputData = Map.of("products", Arrays.asList(product1, product2, product3));

            // Act
            Map<String, Object> result = ruleExecutionService.executeRuleset("test_ruleset", inputData);

            // Assert
            assertThat(result).containsEntry("third_product_info", "Available: Keyboard - $89.99");
        }

        @Test
        @DisplayName("Array index access with bounds checking")
        void testArrayIndexWithBoundsChecking() {
            // Arrange - use size() method to ensure safe array access
            Rule rule = createRule("users.size() > 2 AND users[2].age >= 18",
                    "STRING_CONCAT(\"Third user: \", users[2].name)", "third_user");
            when(ruleRepository.findByRuleset("test_ruleset")).thenReturn(Arrays.asList(rule));

            // Test with insufficient elements - condition should be false
            Map<String, Object> user1 = Map.of("name", "alice", "age", 25);
            Map<String, Object> inputData1 = Map.of("users", Arrays.asList(user1));
            Map<String, Object> result1 = ruleExecutionService.executeRuleset("test_ruleset", inputData1);
            assertThat(result1).doesNotContainKey("third_user");

            // Test with sufficient elements - should work
            Map<String, Object> user2 = Map.of("name", "bob", "age", 30);
            Map<String, Object> user3 = Map.of("name", "charlie", "age", 35);
            Map<String, Object> inputData2 = Map.of("users", Arrays.asList(user1, user2, user3));
            Map<String, Object> result2 = ruleExecutionService.executeRuleset("test_ruleset", inputData2);
            assertThat(result2).containsEntry("third_user", "Third user: charlie");
        }
    }
}