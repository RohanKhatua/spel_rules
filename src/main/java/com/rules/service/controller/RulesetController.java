package com.rules.service.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rules.service.dto.AddRuleRequest;
import com.rules.service.dto.CreateRulesetRequest;
import com.rules.service.dto.ExecuteRulesetRequest;
import com.rules.service.dto.ExecuteRulesetResponse;
import com.rules.service.dto.ExecutionStats;
import com.rules.service.model.Rule;
import com.rules.service.repository.RuleRepository;
import com.rules.service.service.RuleExecutionService;
import com.rules.service.service.RuleParserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/rulesets")
@Tag(name = "Rulesets", description = "Operations related to rulesets")
public class RulesetController {

    private final RuleRepository ruleRepository;
    private final RuleExecutionService ruleExecutionService;
    private final RuleParserService ruleParserService;

    public RulesetController(RuleRepository ruleRepository,
            RuleExecutionService ruleExecutionService,
            RuleParserService ruleParserService) {
        this.ruleRepository = ruleRepository;
        this.ruleExecutionService = ruleExecutionService;
        this.ruleParserService = ruleParserService;
    }

    @Operation(summary = "Create a new ruleset", description = "Creates a new ruleset with the provided rules.", responses = {
            @ApiResponse(responseCode = "200", description = "Ruleset created successfully", content = @Content(schema = @Schema(implementation = Rule.class)))
    })
    @PostMapping
    public ResponseEntity<List<Rule>> createRuleset(@RequestBody CreateRulesetRequest request) {
        List<Rule> rules = request.getRules().stream()
                .map(r -> {
                    RuleParserService.RuleParts parts = ruleParserService.parseRule(r.getRule());
                    return new Rule(parts.getCondition(), parts.getTransformation(), r.getOutputVariable(),
                            request.getName());
                })
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(ruleRepository.saveAll(rules));
    }

    @Operation(summary = "Add a rule to a ruleset", description = "Adds a single rule to an existing ruleset or creates a new ruleset if it doesn't exist. The rule should be in the format 'condition THEN transformation'.", responses = {
            @ApiResponse(responseCode = "200", description = "Rule added successfully", content = @Content(schema = @Schema(implementation = Rule.class))),
            @ApiResponse(responseCode = "400", description = "Invalid rule format")
    })
    @PostMapping("/{name}/rules")
    public ResponseEntity<Rule> addRule(
            @Parameter(description = "Name of the ruleset") @PathVariable("name") String name,
            @RequestBody AddRuleRequest request) {
        try {
            RuleParserService.RuleParts parts = ruleParserService.parseRule(request.getRule());
            Rule rule = new Rule(parts.getCondition(), parts.getTransformation(), request.getOutputVariable(), name);
            return ResponseEntity.ok(ruleRepository.save(rule));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get all rulesets", description = "Retrieves a list of all rulesets.")
    @GetMapping
    public ResponseEntity<List<String>> getAllRulesets() {
        List<String> rulesets = ruleRepository.findAll().stream()
                .map(Rule::getRuleset)
                .distinct()
                .collect(Collectors.toList());
        return ResponseEntity.ok(rulesets);
    }

    @Operation(summary = "Get a ruleset by name", description = "Retrieves a ruleset by its name.")
    @GetMapping("/{name}")
    public ResponseEntity<List<Rule>> getRuleset(
            @Parameter(description = "Name of the ruleset") @PathVariable("name") String name) {
        List<Rule> rules = ruleRepository.findByRuleset(name);
        if (rules.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Execute a ruleset", description = "Executes the ruleset with the given name and input data.")
    @PostMapping("/execute")
    public ResponseEntity<ExecuteRulesetResponse> executeRuleset(
            @RequestBody ExecuteRulesetRequest request) {

        Map<String, Object> outputVariables = ruleExecutionService.executeRuleset(request.getRulesetName(),
                request.getInputData());
        List<Rule> rules = ruleRepository.findByRuleset(request.getRulesetName());

        ExecutionStats stats = new ExecutionStats(
                rules.size(),
                outputVariables.size());

        return ResponseEntity.ok(new ExecuteRulesetResponse(outputVariables, stats));
    }
}