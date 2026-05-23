package com.rulex.repository;

import com.rulex.entity.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface RuleRepository extends JpaRepository<RuleEntity, Long> {
}
