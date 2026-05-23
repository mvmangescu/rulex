package com.rulex.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.rulex.dto.RuleDto;
import com.rulex.engine.CompiledRule;
import com.rulex.entity.RuleEntity;
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
    public RuleDto save(RuleDto dto) {
        return ruleRepository.findByName(dto.name())
                .map(existing -> {
                    ruleCache.invalidate(existing.getExpression());
                    existing.setExpression(dto.expression());
                    existing.setDescription(dto.description());
                    existing.setUpdatedAt(Instant.now());
                    return ruleMapper.toDto(ruleRepository.save(existing));
                })
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    RuleEntity entity = new RuleEntity(dto.name(), dto.expression(), dto.description(), now, now);
                    return ruleMapper.toDto(ruleRepository.save(entity));
                });
    }

    @Transactional(readOnly = true)
    public Optional<RuleDto> find(String name) {
        return ruleRepository.findByName(name).map(ruleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Collection<RuleDto> findAll() {
        return ruleRepository.findAll().stream().map(ruleMapper::toDto).toList();
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
