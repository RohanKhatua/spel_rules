package com.rules.service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rules.service.dto.AddRuleRequest;
import com.rules.service.dto.CreateRulesetRequest;
import com.rules.service.model.Rule;
import com.rules.service.repository.RuleRepository;

/**
 * Service responsible for managing rules and rulesets
 */
@Service
@Transactional
public class RuleService {

    private final RuleRepository ruleRepository;
    private final RuleParserService ruleParserService;

    public RuleService(RuleRepository ruleRepository, RuleParserService ruleParserService) {
        this.ruleRepository = ruleRepository;
        this.ruleParserService = ruleParserService;
    }

    /**
     * Create a new ruleset with multiple rules
     * 
     * @param request The ruleset creation request
     * @return List of created rules
     */
    public List<Rule> createRuleset(CreateRulesetRequest request) {
        List<Rule> rules = request.getRules().stream()
                .map(r -> {
                    RuleParserService.RuleParts parts = ruleParserService.parseRule(r.getRule());
                    return new Rule(parts.getCondition(), parts.getTransformation(), r.getOutputVariable(),
                            request.getName());
                })
                .collect(Collectors.toList());

        return ruleRepository.saveAll(rules);
    }

    /**
     * Add a single rule to a ruleset
     * 
     * @param rulesetName The name of the ruleset
     * @param request     The add rule request
     * @return The created rule
     * @throws IllegalArgumentException if the rule format is invalid
     */
    public Rule addRule(String rulesetName, AddRuleRequest request) throws IllegalArgumentException {
        RuleParserService.RuleParts parts = ruleParserService.parseRule(request.getRule());
        Rule rule = new Rule(parts.getCondition(), parts.getTransformation(), request.getOutputVariable(), rulesetName);
        return ruleRepository.save(rule);
    }

    /**
     * Get all ruleset names
     * 
     * @return List of unique ruleset names
     */
    @Transactional(readOnly = true)
    public List<String> getAllRulesetNames() {
        return ruleRepository.findAll().stream()
                .map(Rule::getRuleset)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get rules for a specific ruleset
     * 
     * @param rulesetName The name of the ruleset
     * @return List of rules in the ruleset
     */
    @Transactional(readOnly = true)
    public List<Rule> getRulesByRuleset(String rulesetName) {
        return ruleRepository.findByRuleset(rulesetName);
    }

    /**
     * Check if a ruleset exists
     * 
     * @param rulesetName The name of the ruleset
     * @return true if the ruleset exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean rulesetExists(String rulesetName) {
        return !ruleRepository.findByRuleset(rulesetName).isEmpty();
    }

    /**
     * Get the count of rules in a ruleset
     * 
     * @param rulesetName The name of the ruleset
     * @return The number of rules in the ruleset
     */
    @Transactional(readOnly = true)
    public int getRuleCountForRuleset(String rulesetName) {
        return ruleRepository.findByRuleset(rulesetName).size();
    }
}