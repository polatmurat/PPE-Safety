package com.ppesafety.api.violation.mapper;

import com.ppesafety.api.violation.dto.ViolationDto;
import com.ppesafety.api.violation.entity.Violation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ViolationMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.fullName")
    @Mapping(target = "reportedById", source = "reportedBy.id")
    @Mapping(target = "reportedByName", source = "reportedBy.fullName")
    ViolationDto toDto(Violation violation);

    List<ViolationDto> toDtoList(List<Violation> violations);
}
