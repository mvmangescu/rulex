package com.rulex.mapper;

import com.rulex.dto.RuleResponse;
import com.rulex.entity.RuleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RuleMapper {

    RuleResponse toRuleResponse(RuleEntity entity);

    @Mapping(target = "id", ignore = true)
    RuleEntity toEntity(RuleResponse dto);
}
