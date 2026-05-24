package com.rulex.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.rulex.dto.RuleResponse;
import com.rulex.engine.CompiledRule;
import com.rulex.entity.RuleEntity;
import com.rulex.exception.NamedRuleNotFoundException;
import com.rulex.mapper.RuleMapper;
import com.rulex.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final Cache<String, CompiledRule> ruleCache;
    private final RuleMapper ruleMapper;

    @Transactional
    public RuleResponse create(RuleResponse dto) {
        Instant now = Instant.now();
        RuleEntity entity = new RuleEntity(dto.name(), dto.expression(), dto.description(), now, now);
        return ruleMapper.toRuleResponse(ruleRepository.save(entity));
    }

    @Transactional
    public RuleResponse update(String name, RuleResponse dto) {
        RuleEntity existing = ruleRepository.findByName(name)
                .orElseThrow(() -> new NamedRuleNotFoundException(name));
        ruleCache.invalidate(existing.getExpression());
        existing.setExpression(dto.expression());
        existing.setDescription(dto.description());
        existing.setUpdatedAt(Instant.now());
        return ruleMapper.toRuleResponse(ruleRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public Optional<RuleResponse> findByName(String name) {
        return ruleRepository.findByName(name).map(ruleMapper::toRuleResponse);
    }

    @Transactional(readOnly = true)
    public Collection<RuleResponse> findAll() {
        return ruleRepository.findAll().stream().map(ruleMapper::toRuleResponse).toList();
    }

    @Transactional
    public boolean delete(String name) {
        return ruleRepository.findByName(name)
                .map(entity -> {
                    ruleCache.invalidate(entity.getExpression());
                    ruleRepository.delete(entity);
                    return true;
                })
                .orElse(false);
    }
}
