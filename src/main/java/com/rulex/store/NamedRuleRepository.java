package com.rulex.store;

import org.springframework.data.jpa.repository.JpaRepository;

interface NamedRuleRepository extends JpaRepository<NamedRuleEntity, String> {}
