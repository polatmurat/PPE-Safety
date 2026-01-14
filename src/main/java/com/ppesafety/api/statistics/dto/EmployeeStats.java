package com.ppesafety.api.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStats implements Serializable {

    private Long employeeId;
    private String employeeName;
    private long totalViolations;
    private long violationsThisWeek;
    private long violationsThisMonth;
}
