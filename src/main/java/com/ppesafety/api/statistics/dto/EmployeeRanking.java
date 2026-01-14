package com.ppesafety.api.statistics.dto;

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
public class EmployeeRanking implements Serializable {

    private List<RankedEmployee> topViolators;
    private List<RankedEmployee> leastViolators;
    private long totalEmployees;
    private double averageViolationsPerEmployee;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankedEmployee implements Serializable {
        private Long employeeId;
        private String employeeName;
        private String email;
        private long violationCount;
        private int rank;
    }
}
