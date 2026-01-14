package com.ppesafety.api.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesStats implements Serializable {

    private LocalDate startDate;
    private LocalDate endDate;
    private List<DailyCount> dailyCounts;
    private Map<String, List<DailyCount>> byLabel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCount implements Serializable {
        private LocalDate date;
        private long count;
    }
}
