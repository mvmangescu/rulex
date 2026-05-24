package com.rulex.mapper;

import com.rulex.dto.RuleResponse;
import com.rulex.entity.RuleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RuleMapper {

    RuleResponse toRuleResponse(RuleEntity entity);
}
