package com.rulex.mapper;

import com.rulex.dto.CreateRuleRequest;
import com.rulex.dto.RuleResponse;
import com.rulex.dto.UpdateRuleRequest;
import com.rulex.entity.RuleEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RuleMapper {

    RuleResponse toRuleResponse(RuleEntity entity);

    List<RuleResponse> toRuleResponseList(List<RuleEntity> entities);

    @Mapping(target = "id", ignore = true)
    RuleEntity toEntity(CreateRuleRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(UpdateRuleRequest request, @MappingTarget RuleEntity entity);
}
