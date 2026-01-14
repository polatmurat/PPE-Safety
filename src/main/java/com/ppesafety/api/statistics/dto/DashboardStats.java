package com.ppesafety.api.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats implements Serializable {

    private long totalViolations;
    private long violationsThisWeek;
    private long violationsThisMonth;
    private String mostViolatedRule;
    private long mostViolatedRuleCount;
    private Map<String, Long> violationsByLabel;
    private Map<String, Long> topViolators;
}
