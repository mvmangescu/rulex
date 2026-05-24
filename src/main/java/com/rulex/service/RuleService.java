package com.rulex.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.rulex.dto.CreateRuleRequest;
import com.rulex.dto.RuleResponse;
import com.rulex.dto.UpdateRuleRequest;
import com.rulex.engine.CompiledRule;
import com.rulex.entity.RuleEntity;
import com.rulex.exception.RuleNotFoundException;
import com.rulex.mapper.RuleMapper;
import com.rulex.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final Cache<String, CompiledRule> ruleCache;
    private final RuleMapper ruleMapper;

    @Transactional
    public RuleResponse create(CreateRuleRequest request) {
        RuleEntity entity = ruleMapper.toEntity(request);
        return ruleMapper.toRuleResponse(ruleRepository.save(entity));
    }

    @Transactional
    public RuleResponse update(Long id, UpdateRuleRequest request) {
        RuleEntity existing = ruleRepository.findById(id).orElseThrow(() -> new RuleNotFoundException(id));
        ruleCache.invalidate(existing.getExpression());
        ruleMapper.update(request, existing);
        return ruleMapper.toRuleResponse(ruleRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public Optional<RuleResponse> findById(Long id) {
        return ruleRepository.findById(id).map(ruleMapper::toRuleResponse);
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> findAll() {
        return ruleMapper.toRuleResponseList(ruleRepository.findAll());
    }

    @Transactional
    public boolean deleteById(Long id) {
        return ruleRepository.findById(id)
                .map(entity -> {
                    ruleCache.invalidate(entity.getExpression());
                    ruleRepository.delete(entity);
                    return true;
                }).orElse(false);
    }
}
