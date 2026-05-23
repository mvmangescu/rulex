package com.rulex.mapper;

import com.rulex.dto.RuleDto;
import com.rulex.entity.RuleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RuleMapper {

    RuleDto toDto(RuleEntity entity);
}
