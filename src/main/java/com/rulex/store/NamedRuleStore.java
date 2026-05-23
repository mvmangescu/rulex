package com.rulex.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.rulex.engine.CompiledRule;
import com.rulex.entity.NamedRuleEntity;
import com.rulex.repository.NamedRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

@Service
public class NamedRuleStore {

    public record SaveResult(NamedRule rule, boolean created) {}

    private final NamedRuleRepository repository;
    private final Cache<String, CompiledRule> ruleCache;

    public NamedRuleStore(NamedRuleRepository repository, Cache<String, CompiledRule> ruleCache) {
        this.repository = repository;
        this.ruleCache = ruleCache;
    }

    @Transactional
    public SaveResult save(String name, String expression) {
        return repository.findById(name)
                .map(existing -> {
                    ruleCache.invalidate(existing.getExpression());
                    existing.setExpression(expression);
                    existing.setUpdatedAt(Instant.now());
                    return new SaveResult(repository.save(existing).toDomain(), false);
                })
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    NamedRule rule = repository.save(new NamedRuleEntity(name, expression, now, now)).toDomain();
                    return new SaveResult(rule, true);
                });
    }

    @Transactional(readOnly = true)
    public Optional<NamedRule> find(String name) {
        return repository.findById(name).map(NamedRuleEntity::toDomain);
    }

    @Transactional(readOnly = true)
    public Collection<NamedRule> findAll() {
        return repository.findAll().stream().map(NamedRuleEntity::toDomain).toList();
    }

    @Transactional
    public boolean delete(String name) {
        return repository.findById(name)
                .map(entity -> {
                    ruleCache.invalidate(entity.getExpression());
                    repository.delete(entity);
                    return true;
                })
                .orElse(false);
    }
}
