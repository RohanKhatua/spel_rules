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
import com.rules.service.repository.RuleRepository;
import com.rules.service.service.NestedMapPropertyAccessor;
import com.rules.service.service.PropertyAccessWrapper;
import com.rules.service.service.PropertyAccessWrapperAccessor;
import com.rules.service.service.RuleExecutionService;
import com.rules.service.service.SpelContextConfigurationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Array Index Access Demo Test")
public class ArrayIndexDemoTest {

        @Mock
        private RuleRepository ruleRepository;

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
                rule.setRuleset("demo_ruleset");
                return rule;
        }

        @Test
        @DisplayName("Demo: E-commerce order processing with array index access")
        void testEcommerceOrderProcessing() {
                // Arrange
                Rule rule = createRule(
                                "orders.size() > 0 AND orders[0].status == \"shipped\" AND orders[0].amount >= 100",
                                "STRING_CONCAT(\"Order #\", orders[0].id.toString(), \" for $\", orders[0].amount.toString(), \" has been shipped to \", orders[0].customer.name)",
                                "shipping_notification");
                when(ruleRepository.findByRuleset("demo_ruleset")).thenReturn(Arrays.asList(rule));

                // Create sample e-commerce data
                Map<String, Object> customer = Map.of("name", "John Doe", "email", "john@example.com");
                Map<String, Object> order1 = Map.of(
                                "id", 12345,
                                "amount", 299.99,
                                "status", "shipped",
                                "customer", customer);
                Map<String, Object> order2 = Map.of(
                                "id", 12346,
                                "amount", 59.99,
                                "status", "pending",
                                "customer", customer);
                Map<String, Object> inputData = Map.of("orders", Arrays.asList(order1, order2));

                // Act
                Map<String, Object> result = ruleExecutionService.executeRuleset("demo_ruleset", inputData);

                // Assert
                assertThat(result).containsEntry("shipping_notification",
                                "Order #12345 for $299.99 has been shipped to John Doe");
        }

        @Test
        @DisplayName("Demo: Employee management with nested array access")
        void testEmployeeManagement() {
                // Arrange
                Rule rule = createRule(
                                "company.employees.size() > 1 AND company.employees[1].department.name == \"Engineering\"",
                                "STRING_CONCAT(company.employees[1].profile.firstName, \" \", company.employees[1].profile.lastName, \" works in \", company.employees[1].department.name)",
                                "employee_info");
                when(ruleRepository.findByRuleset("demo_ruleset")).thenReturn(Arrays.asList(rule));

                // Create complex nested company data
                Map<String, Object> profile1 = Map.of("firstName", "Alice", "lastName", "Johnson");
                Map<String, Object> profile2 = Map.of("firstName", "Bob", "lastName", "Smith");
                Map<String, Object> dept1 = Map.of("name", "HR", "budget", 50000);
                Map<String, Object> dept2 = Map.of("name", "Engineering", "budget", 200000);

                Map<String, Object> employee1 = Map.of("profile", profile1, "department", dept1);
                Map<String, Object> employee2 = Map.of("profile", profile2, "department", dept2);

                Map<String, Object> company = Map.of(
                                "name", "TechCorp",
                                "employees", Arrays.asList(employee1, employee2));
                Map<String, Object> inputData = Map.of("company", company);

                // Act
                Map<String, Object> result = ruleExecutionService.executeRuleset("demo_ruleset", inputData);

                // Assert
                assertThat(result).containsEntry("employee_info", "Bob Smith works in Engineering");
        }

        @Test
        @DisplayName("Demo: Safe array access with bounds checking")
        void testSafeArrayAccess() {
                // Arrange
                Rule rule = createRule(
                                "products.size() >= 3 AND products[2].inStock == true AND products[2].price <= 100",
                                "STRING_CONCAT(\"Special offer: \", products[2].name, \" available for $\", products[2].price.toString())",
                                "special_offer");
                when(ruleRepository.findByRuleset("demo_ruleset")).thenReturn(Arrays.asList(rule));

                // Test with insufficient products - should not trigger
                Map<String, Object> product1 = Map.of("name", "Laptop", "price", 999.99, "inStock", true);
                Map<String, Object> inputData1 = Map.of("products", Arrays.asList(product1));
                Map<String, Object> result1 = ruleExecutionService.executeRuleset("demo_ruleset", inputData1);
                assertThat(result1).doesNotContainKey("special_offer");

                // Test with sufficient products - should trigger
                Map<String, Object> product2 = Map.of("name", "Mouse", "price", 29.99, "inStock", true);
                Map<String, Object> product3 = Map.of("name", "Keyboard", "price", 79.99, "inStock", true);
                Map<String, Object> inputData2 = Map.of("products", Arrays.asList(product1, product2, product3));
                Map<String, Object> result2 = ruleExecutionService.executeRuleset("demo_ruleset", inputData2);
                assertThat(result2).containsEntry("special_offer", "Special offer: Keyboard available for $79.99");
        }
}