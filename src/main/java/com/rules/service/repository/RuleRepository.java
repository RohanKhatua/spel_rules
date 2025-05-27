package com.rules.service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rules.service.model.Rule;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {
    List<Rule> findByRuleset(String ruleset);
}