package com.rulex.repository;

import com.rulex.entity.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RuleRepository extends JpaRepository<RuleEntity, Long> {

    Optional<RuleEntity> findByName(String name);
}
