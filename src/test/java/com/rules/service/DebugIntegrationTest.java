package com.rules.service;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rules.service.repository.RuleRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DebugIntegrationTest {

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ruleRepository.deleteAll();
    }

    @Test
    void debugSimpleRuleCreationAndExecution() throws Exception {
        // Step 1: Create a simple ruleset
        String rulesetPayload = """
                {
                    "name": "debug_test",
                    "rules": [
                        {
                            "rule": "age >= 18 THEN STRING_UPPERCASE(name)",
                            "outputVariable": "name_upper"
                        }
                    ]
                }
                """;

        System.out.println("Creating ruleset with payload: " + rulesetPayload);

        MvcResult createResult = mockMvc.perform(post("/api/rulesets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rulesetPayload))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        System.out.println("Create response status: " + createResult.getResponse().getStatus());
        System.out.println("Create response body: " + createResult.getResponse().getContentAsString());

        // Step 2: Execute the ruleset
        String executePayload = """
                {
                    "rulesetName": "debug_test",
                    "inputData": {
                        "name": "alice",
                        "age": 25
                    }
                }
                """;

        System.out.println("Executing ruleset with payload: " + executePayload);

        MvcResult executeResult = mockMvc.perform(post("/api/rulesets/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(executePayload))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        System.out.println("Execute response status: " + executeResult.getResponse().getStatus());
        System.out.println("Execute response body: " + executeResult.getResponse().getContentAsString());

        // Step 3: Parse and verify the response
        String responseBody = executeResult.getResponse().getContentAsString();
        TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
        };
        Map<String, Object> response = objectMapper.readValue(responseBody, typeRef);

        System.out.println("Parsed response: " + response);

        if (response.containsKey("outputVariables")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outputVariables = (Map<String, Object>) response.get("outputVariables");
            System.out.println("Output variables: " + outputVariables);

            Object nameUpper = outputVariables.get("name_upper");
            System.out.println("name_upper value: " + nameUpper);
            System.out.println("name_upper type: " + (nameUpper != null ? nameUpper.getClass() : "null"));

            assertThat(nameUpper).isEqualTo("ALICE");
        } else {
            System.out.println("No outputVariables found in response!");
            System.out.println("Available keys: " + response.keySet());
        }
    }
}