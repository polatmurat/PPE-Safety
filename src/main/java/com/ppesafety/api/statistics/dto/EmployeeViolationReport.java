package com.ppesafety.api.statistics.dto;

import com.ppesafety.api.violation.dto.ViolationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeViolationReport implements Serializable {

    private Long employeeId;
    private String employeeName;
    private String email;
    private EmployeeStats stats;
    private List<ViolationDto> recentViolations;
    private List<String> mostFrequentLabels;
}
