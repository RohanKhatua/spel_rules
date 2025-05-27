package com.rules.service;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rules.service.repository.RuleRepository;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Rule Execution Service - Comprehensive Integration Tests")
public class RuleExecutionServiceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        ruleRepository.deleteAll();
    }

    @Nested
    @DisplayName("Basic String Transformations")
    class BasicStringTransformations {

        @Test
        @DisplayName("STRING_UPPERCASE transformation should convert name to uppercase")
        void testStringUppercaseTransformation() throws Exception {
            // Create ruleset and rule
            String rulesetPayload = """
                    {
                        "name": "string_test",
                        "rules": [
                            {
                                "rule": "age >= 18 THEN STRING_UPPERCASE(name)",
                                "outputVariable": "name_upper"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute rule
            String executePayload = """
                    {
                        "rulesetName": "string_test",
                        "inputData": {
                            "name": "alice",
                            "age": 25
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("name_upper")).isEqualTo("ALICE");
        }

        @Test
        @DisplayName("STRING_LOWERCASE transformation should convert name to lowercase")
        void testStringLowercaseTransformation() throws Exception {
            // Create rule
            createRule("string_test", "age >= 18 THEN STRING_LOWERCASE(name)", "name_lower");

            // Execute
            Map<String, Object> result = executeRuleset("string_test", Map.of("name", "JOHN", "age", 30));

            assertThat(result.get("name_lower")).isEqualTo("john");
        }

        @Test
        @DisplayName("STRING_CONCAT transformation should concatenate strings")
        void testStringConcatTransformation() throws Exception {
            // Create rule
            createRule("string_test", "age >= 18 THEN STRING_CONCAT(\"Hello, \", name)", "greeting");

            // Execute
            Map<String, Object> result = executeRuleset("string_test", Map.of("name", "world", "age", 25));

            assertThat(result.get("greeting")).isEqualTo("Hello, world");
        }

        @Test
        @DisplayName("STRING_SUBSTRING transformation should extract substring")
        void testStringSubstringTransformation() throws Exception {
            // Create rule
            createRule("string_test", "name.length() > 5 THEN STRING_SUBSTRING(name, 0, 3)", "name_short");

            // Execute
            Map<String, Object> result = executeRuleset("string_test", Map.of("name", "Alexander", "age", 25));

            assertThat(result.get("name_short")).isEqualTo("Ale");
        }
    }

    @Nested
    @DisplayName("Nested Transformations")
    class NestedTransformations {

        @Test
        @DisplayName("Nested STRING_UPPERCASE(STRING_CONCAT()) should work correctly")
        void testNestedUppercaseConcat() throws Exception {
            // Create rule with nested transformation
            createRule("nested_test",
                    "age >= 18 THEN STRING_UPPERCASE(STRING_CONCAT(\"Mr. \", name))",
                    "formal_name");

            // Execute
            Map<String, Object> result = executeRuleset("nested_test", Map.of("name", "smith", "age", 30));

            assertThat(result.get("formal_name")).isEqualTo("MR. SMITH");
        }

        @Test
        @DisplayName("Nested STRING_CONCAT(STRING_UPPERCASE(), STRING_LOWERCASE()) should work")
        void testNestedConcatWithMixedCase() throws Exception {
            // Create rule with complex nested transformation
            createRule("nested_test",
                    "name.length() > 3 THEN STRING_CONCAT(STRING_UPPERCASE(STRING_SUBSTRING(name, 0, 1)), STRING_LOWERCASE(STRING_SUBSTRING(name, 1, name.length())))",
                    "proper_case");

            // Execute
            Map<String, Object> result = executeRuleset("nested_test", Map.of("name", "jOHN", "age", 25));

            assertThat(result.get("proper_case")).isEqualTo("John");
        }

        @Test
        @DisplayName("Triple nested transformation should work")
        void testTripleNestedTransformation() throws Exception {
            // Create rule with triple nesting
            createRule("nested_test",
                    "age >= 21 THEN STRING_UPPERCASE(STRING_CONCAT(\"DR. \", STRING_LOWERCASE(name)))",
                    "doctor_title");

            // Execute
            Map<String, Object> result = executeRuleset("nested_test", Map.of("name", "WATSON", "age", 35));

            assertThat(result.get("doctor_title")).isEqualTo("DR. watson");
        }
    }

    @Nested
    @DisplayName("Complex Conditions with Transformations")
    class ComplexConditionsWithTransformations {

        @Test
        @DisplayName("Multiple conditions with logical operators")
        void testComplexConditionsWithAnd() throws Exception {
            createRule("complex_test",
                    "age >= 18 AND name.length() > 3 AND age < 65 THEN STRING_CONCAT(name, \" - Valid Adult\")",
                    "status_message");

            // Test valid case
            Map<String, Object> result1 = executeRuleset("complex_test", Map.of("name", "Alice", "age", 25));
            assertThat(result1.get("status_message")).isEqualTo("Alice - Valid Adult");

            // Test invalid case (too young)
            Map<String, Object> result2 = executeRuleset("complex_test", Map.of("name", "Bob", "age", 16));
            assertThat(result2.get("status_message")).isNull();

            // Test invalid case (name too short)
            Map<String, Object> result3 = executeRuleset("complex_test", Map.of("name", "Jo", "age", 25));
            assertThat(result3.get("status_message")).isNull();
        }

        @Test
        @DisplayName("Conditions with OR operator")
        void testComplexConditionsWithOr() throws Exception {
            createRule("complex_test",
                    "age >= 65 OR age <= 12 THEN STRING_CONCAT(name, \" - Special Rate\")",
                    "rate_category");

            // Test senior citizen
            Map<String, Object> result1 = executeRuleset("complex_test", Map.of("name", "George", "age", 70));
            assertThat(result1.get("rate_category")).isEqualTo("George - Special Rate");

            // Test child
            Map<String, Object> result2 = executeRuleset("complex_test", Map.of("name", "Emma", "age", 8));
            assertThat(result2.get("rate_category")).isEqualTo("Emma - Special Rate");

            // Test regular adult
            Map<String, Object> result3 = executeRuleset("complex_test", Map.of("name", "John", "age", 35));
            assertThat(result3.get("rate_category")).isNull();
        }
    }

    @Nested
    @DisplayName("Built-in String Methods")
    class BuiltInStringMethods {

        @Test
        @DisplayName("Using native string methods without # prefix")
        void testNativeStringMethods() throws Exception {
            createRule("native_test",
                    "name.length() > 5 THEN name.toUpperCase()",
                    "long_name_upper");

            Map<String, Object> result = executeRuleset("native_test", Map.of("name", "alexander", "age", 25));
            assertThat(result.get("long_name_upper")).isEqualTo("ALEXANDER");
        }

        @Test
        @DisplayName("Combining native methods with custom functions")
        void testMixedMethods() throws Exception {
            createRule("mixed_test",
                    "name.length() > 3 THEN STRING_CONCAT(name.substring(0, 1).toUpperCase(), name.substring(1).toLowerCase())",
                    "title_case");

            Map<String, Object> result = executeRuleset("mixed_test", Map.of("name", "mARY", "age", 28));
            assertThat(result.get("title_case")).isEqualTo("Mary");
        }
    }

    @Nested
    @DisplayName("Multiple Rules in Ruleset")
    class MultipleRulesInRuleset {

        @Test
        @DisplayName("Multiple rules should execute in sequence")
        void testMultipleRulesExecution() throws Exception {
            String rulesetPayload = """
                    {
                        "name": "multi_rules_test",
                        "rules": [
                            {
                                "rule": "age >= 18 THEN \\\"ADULT\\\"",
                                "outputVariable": "age_category"
                            },
                            {
                                "rule": "age >= 18 THEN STRING_UPPERCASE(name)",
                                "outputVariable": "name_upper"
                            },
                            {
                                "rule": "age >= 21 THEN STRING_CONCAT(name, \\\" can drink\\\")",
                                "outputVariable": "drink_status"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            Map<String, Object> result = executeRuleset("multi_rules_test", Map.of("name", "john", "age", 25));

            assertThat(result.get("age_category")).isEqualTo("ADULT");
            assertThat(result.get("name_upper")).isEqualTo("JOHN");
            assertThat(result.get("drink_status")).isEqualTo("john can drink");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Null handling in transformations")
        void testNullHandling() throws Exception {
            createRule("null_test",
                    "age >= 18 THEN STRING_UPPERCASE(name)",
                    "name_upper");

            // Test with null name - should handle gracefully
            Map<String, Object> result = executeRuleset("null_test", Map.of("name", (String) null, "age", 25));
            assertThat(result.get("name_upper")).isNull();
        }

        @Test
        @DisplayName("Empty string handling")
        void testEmptyStringHandling() throws Exception {
            createRule("empty_test",
                    "age >= 18 THEN STRING_CONCAT(\"Hello, \", name)",
                    "greeting");

            Map<String, Object> result = executeRuleset("empty_test", Map.of("name", "", "age", 25));
            assertThat(result.get("greeting")).isEqualTo("Hello, ");
        }

        @Test
        @DisplayName("Condition evaluates to false - no transformation should occur")
        void testFalseConditionNoTransformation() throws Exception {
            createRule("false_condition_test",
                    "age >= 21 THEN STRING_UPPERCASE(name)",
                    "name_upper");

            Map<String, Object> result = executeRuleset("false_condition_test", Map.of("name", "john", "age", 18));
            assertThat(result.get("name_upper")).isNull();
        }
    }

    @Nested
    @DisplayName("Advanced Data Types")
    class AdvancedDataTypes {

        @Test
        @DisplayName("Working with numeric values in transformations")
        void testNumericValues() throws Exception {
            createRule("numeric_test",
                    "age >= 18 THEN STRING_CONCAT(name, \" is \", age.toString(), \" years old\")",
                    "age_description");

            Map<String, Object> result = executeRuleset("numeric_test", Map.of("name", "Alice", "age", 25));
            assertThat(result.get("age_description")).isEqualTo("Alice is 25 years old");
        }

        @Test
        @DisplayName("Working with boolean values")
        void testBooleanValues() throws Exception {
            createRule("boolean_test",
                    "isActive == true AND age >= 18 THEN STRING_CONCAT(name, \" - Active Adult\")",
                    "status");

            Map<String, Object> result = executeRuleset("boolean_test",
                    Map.of("name", "Bob", "age", 30, "isActive", true));
            assertThat(result.get("status")).isEqualTo("Bob - Active Adult");
        }
    }

    @Nested
    @DisplayName("Performance and Stress Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Complex nested transformations should execute efficiently")
        void testComplexNestedPerformance() throws Exception {
            createRule("performance_test",
                    "age >= 18 THEN STRING_UPPERCASE(STRING_CONCAT(STRING_CONCAT(\"Dr. \", STRING_LOWERCASE(name)), STRING_CONCAT(\" - Age: \", age.toString())))",
                    "complex_title");

            long startTime = System.currentTimeMillis();

            Map<String, Object> result = executeRuleset("performance_test",
                    Map.of("name", "EINSTEIN", "age", 42));

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            assertThat(result.get("complex_title")).isEqualTo("DR. EINSTEIN - AGE: 42");
            assertThat(executionTime).isLessThan(1000); // Should execute in less than 1 second
        }
    }

    @Nested
    @DisplayName("Nested JSON Object Integration Tests")
    class NestedJsonObjectIntegrationTests {

        @Test
        @DisplayName("Basic nested object access through REST API")
        void testBasicNestedObjectAccess() throws Exception {
            // Create ruleset with nested object access
            String rulesetPayload = """
                    {
                        "name": "nested_basic_test",
                        "rules": [
                            {
                                "rule": "user.age >= 18 THEN STRING_UPPERCASE(user.name)",
                                "outputVariable": "user_name_upper"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute with nested JSON input
            String executePayload = """
                    {
                        "rulesetName": "nested_basic_test",
                        "inputData": {
                            "user": {
                                "name": "alice",
                                "age": 25
                            }
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("user_name_upper")).isEqualTo("ALICE");
        }

        @Test
        @DisplayName("Deep nested object access through REST API")
        void testDeepNestedObjectAccess() throws Exception {
            // Create ruleset with deep nested access
            String rulesetPayload = """
                    {
                        "name": "nested_deep_test",
                        "rules": [
                            {
                                "rule": "user.profile.age >= 21 THEN STRING_CONCAT(user.profile.firstName, \\\" \\\", user.profile.lastName)",
                                "outputVariable": "full_name"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute with deeply nested JSON input
            String executePayload = """
                    {
                        "rulesetName": "nested_deep_test",
                        "inputData": {
                            "user": {
                                "profile": {
                                    "firstName": "John",
                                    "lastName": "Doe",
                                    "age": 30
                                }
                            }
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("full_name")).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Multiple nested objects in same rule through REST API")
        void testMultipleNestedObjects() throws Exception {
            // Create ruleset with multiple nested objects
            String rulesetPayload = """
                    {
                        "name": "nested_multiple_test",
                        "rules": [
                            {
                                "rule": "user.age >= 18 AND address.country == \\\"USA\\\" THEN STRING_CONCAT(user.name, \\\" from \\\", address.city)",
                                "outputVariable": "user_location"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute with multiple nested objects
            String executePayload = """
                    {
                        "rulesetName": "nested_multiple_test",
                        "inputData": {
                            "user": {
                                "name": "Bob",
                                "age": 25
                            },
                            "address": {
                                "city": "New York",
                                "country": "USA"
                            }
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("user_location")).isEqualTo("Bob from New York");
        }

        @Test
        @DisplayName("Complex nested structure with arrays through REST API")
        void testComplexNestedStructure() throws Exception {
            // Create ruleset with complex nested structure
            String rulesetPayload = """
                    {
                        "name": "nested_complex_test",
                        "rules": [
                            {
                                "rule": "company.employees.size() > 0 AND company.active == true THEN STRING_CONCAT(company.name, \\\" has \\\", company.employees.size().toString(), \\\" employees\\\")",
                                "outputVariable": "company_info"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute with complex nested structure
            String executePayload = """
                    {
                        "rulesetName": "nested_complex_test",
                        "inputData": {
                            "company": {
                                "name": "TechCorp",
                                "active": true,
                                "employees": ["Alice", "Bob", "Charlie"]
                            }
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("company_info")).isEqualTo("TechCorp has 3 employees");
        }

        @Test
        @DisplayName("Mixed flat and nested property access through REST API")
        void testMixedFlatAndNestedAccess() throws Exception {
            // Create ruleset with mixed access patterns
            String rulesetPayload = """
                    {
                        "name": "nested_mixed_test",
                        "rules": [
                            {
                                "rule": "age >= 18 AND user.profile.verified == true THEN STRING_CONCAT(name, \\\" - \\\", user.profile.title)",
                                "outputVariable": "verified_user"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute with mixed flat and nested data
            String executePayload = """
                    {
                        "rulesetName": "nested_mixed_test",
                        "inputData": {
                            "name": "Alice",
                            "age": 30,
                            "user": {
                                "profile": {
                                    "title": "Senior Developer",
                                    "verified": true
                                }
                            }
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("verified_user")).isEqualTo("Alice - Senior Developer");
        }
    }

    @Nested
    @DisplayName("Array Index Access Integration Tests")
    class ArrayIndexAccessIntegrationTests {

        @Test
        @DisplayName("Basic array index access through REST API")
        void testBasicArrayIndexAccess() throws Exception {
            // Create ruleset with array index access
            String rulesetPayload = """
                    {
                        "name": "array_basic_test",
                        "rules": [
                            {
                                "rule": "users[0].age >= 18 THEN STRING_UPPERCASE(users[0].name)",
                                "outputVariable": "first_user_name_upper"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute with array input
            String executePayload = """
                    {
                        "rulesetName": "array_basic_test",
                        "inputData": {
                            "users": [
                                {
                                    "name": "alice",
                                    "age": 25
                                },
                                {
                                    "name": "bob",
                                    "age": 30
                                }
                            ]
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("first_user_name_upper")).isEqualTo("ALICE");
        }

        @Test
        @DisplayName("Deep nested array index access through REST API")
        void testDeepNestedArrayIndexAccess() throws Exception {
            // Create ruleset with deep nested array access
            String rulesetPayload = """
                    {
                        "name": "array_deep_test",
                        "rules": [
                            {
                                "rule": "company.employees[1].profile.active == true THEN STRING_CONCAT(company.employees[1].profile.firstName, \\\" \\\", company.employees[1].profile.lastName)",
                                "outputVariable": "second_employee_name"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute with deeply nested array input
            String executePayload = """
                    {
                        "rulesetName": "array_deep_test",
                        "inputData": {
                            "company": {
                                "employees": [
                                    {
                                        "profile": {
                                            "firstName": "Alice",
                                            "lastName": "Smith",
                                            "active": false
                                        }
                                    },
                                    {
                                        "profile": {
                                            "firstName": "Bob",
                                            "lastName": "Johnson",
                                            "active": true
                                        }
                                    }
                                ]
                            }
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("second_employee_name")).isEqualTo("Bob Johnson");
        }

        @Test
        @DisplayName("Multiple array index accesses through REST API")
        void testMultipleArrayIndexAccesses() throws Exception {
            // Create ruleset with multiple array accesses
            String rulesetPayload = """
                    {
                        "name": "array_multiple_test",
                        "rules": [
                            {
                                "rule": "orders[0].amount >= 100 AND orders[1].status == \\\"pending\\\" THEN STRING_CONCAT(\\\"Order \\\", orders[0].id.toString(), \\\" and Order \\\", orders[1].id.toString())",
                                "outputVariable": "order_comparison"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute with multiple orders
            String executePayload = """
                    {
                        "rulesetName": "array_multiple_test",
                        "inputData": {
                            "orders": [
                                {
                                    "id": 123,
                                    "amount": 150.50,
                                    "status": "completed"
                                },
                                {
                                    "id": 124,
                                    "amount": 75.25,
                                    "status": "pending"
                                }
                            ]
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("order_comparison")).isEqualTo("Order 123 and Order 124");
        }

        @Test
        @DisplayName("Array index with mixed flat and nested properties through REST API")
        void testArrayIndexWithMixedProperties() throws Exception {
            // Create ruleset mixing array index and regular nested access
            String rulesetPayload = """
                    {
                        "name": "array_mixed_test",
                        "rules": [
                            {
                                "rule": "customerName != null AND orders[0].status == \\\"shipped\\\" THEN STRING_CONCAT(customerName, \\\" - Order \\\", orders[0].trackingNumber)",
                                "outputVariable": "shipping_info"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute with mixed data structure
            String executePayload = """
                    {
                        "rulesetName": "array_mixed_test",
                        "inputData": {
                            "customerName": "John Doe",
                            "orders": [
                                {
                                    "status": "shipped",
                                    "trackingNumber": "TRK123456"
                                }
                            ]
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("shipping_info")).isEqualTo("John Doe - Order TRK123456");
        }

        @Test
        @DisplayName("Array index with bounds checking through REST API")
        void testArrayIndexBoundsChecking() throws Exception {
            // Create ruleset with safe bounds checking
            String rulesetPayload = """
                    {
                        "name": "array_bounds_test",
                        "rules": [
                            {
                                "rule": "users.size() > 1 AND users[1].age >= 18 THEN STRING_UPPERCASE(users[1].name)",
                                "outputVariable": "second_user"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/api/rulesets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rulesetPayload))
                    .andExpect(status().isCreated());

            // Execute with sufficient array size
            String executePayload = """
                    {
                        "rulesetName": "array_bounds_test",
                        "inputData": {
                            "users": [
                                {
                                    "name": "alice",
                                    "age": 25
                                },
                                {
                                    "name": "bob",
                                    "age": 30
                                }
                            ]
                        }
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");

            assertThat(outputVariables.get("second_user")).isEqualTo("BOB");
        }
    }

    // Helper methods
    private void createRule(String rulesetName, String rule, String outputVariable) throws Exception {
        String rulePayload = String.format("""
                {
                    "name": "%s",
                    "rules": [
                        {
                            "rule": "%s",
                            "outputVariable": "%s"
                        }
                    ]
                }
                """, rulesetName, rule.replace("\"", "\\\""), outputVariable);

        mockMvc.perform(post("/api/rulesets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rulePayload))
                .andExpect(status().isCreated());
    }

    private Map<String, Object> executeRuleset(String rulesetName, Map<String, Object> inputData) throws Exception {
        String executePayload = String.format("""
                {
                    "rulesetName": "%s",
                    "inputData": %s
                }
                """, rulesetName, objectMapper.writeValueAsString(inputData));

        MvcResult result = mockMvc.perform(post("/api/rulesets/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(executePayload))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
        };
        Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);
        @SuppressWarnings("unchecked")
        Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");
        return outputVariables;
    }
}