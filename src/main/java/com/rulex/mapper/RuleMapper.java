package com.rulex.mapper;

import com.rulex.dto.RuleResponse;
import com.rulex.entity.RuleEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RuleMapper {

    RuleResponse toRuleResponse(RuleEntity entity);

    @Mapping(target = "id", ignore = true)
    RuleEntity toEntity(RuleResponse dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", ignore = true)
    void applyNonNull(RuleResponse dto, @MappingTarget RuleEntity entity);
}
