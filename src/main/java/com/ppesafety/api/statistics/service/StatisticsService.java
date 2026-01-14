package com.ppesafety.api.statistics.service;

import com.ppesafety.api.core.exception.ResourceNotFoundException;
import com.ppesafety.api.statistics.dto.*;
import com.ppesafety.api.user.entity.Role;
import com.ppesafety.api.user.entity.User;
import com.ppesafety.api.user.repository.UserRepository;
import com.ppesafety.api.violation.dto.ViolationDto;
import com.ppesafety.api.violation.entity.Violation;
import com.ppesafety.api.violation.mapper.ViolationMapper;
import com.ppesafety.api.violation.repository.ViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);

    private final ViolationRepository violationRepository;
    private final UserRepository userRepository;
    private final ViolationMapper violationMapper;

    public StatisticsService(ViolationRepository violationRepository,
            UserRepository userRepository,
            ViolationMapper violationMapper) {
        this.violationRepository = violationRepository;
        this.userRepository = userRepository;
        this.violationMapper = violationMapper;
    }

    @Cacheable(value = "statistics", key = "'dashboard'")
    public DashboardStats getDashboardStats() {
        logger.info("Calculating dashboard statistics (cache miss)");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthStart = now.withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);

        long totalViolations = violationRepository.count();
        long violationsThisWeek = violationRepository.countByTimestampBetween(weekStart, now);
        long violationsThisMonth = violationRepository.countByTimestampBetween(monthStart, now);

        Map<String, Long> violationsByLabel = getViolationsByLabel(monthStart, now);

        String mostViolatedRule = null;
        long mostViolatedRuleCount = 0;
        for (Map.Entry<String, Long> entry : violationsByLabel.entrySet()) {
            if (entry.getValue() > mostViolatedRuleCount) {
                mostViolatedRule = entry.getKey();
                mostViolatedRuleCount = entry.getValue();
            }
        }

        Map<String, Long> topViolators = getTopViolators(monthStart, now, 5);

        return DashboardStats.builder()
                .totalViolations(totalViolations)
                .violationsThisWeek(violationsThisWeek)
                .violationsThisMonth(violationsThisMonth)
                .mostViolatedRule(mostViolatedRule)
                .mostViolatedRuleCount(mostViolatedRuleCount)
                .violationsByLabel(violationsByLabel)
                .topViolators(topViolators)
                .build();
    }

    @Cacheable(value = "statistics", key = "'employee-' + #employeeId")
    public EmployeeStats getEmployeeStats(Long employeeId) {
        logger.info("Calculating employee statistics for {} (cache miss)", employeeId);

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthStart = now.withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);

        long totalViolations = violationRepository.countByEmployeeId(employeeId);
        long violationsThisWeek = violationRepository.countByEmployeeIdAndTimestampBetween(
                employeeId, weekStart, now);
        long violationsThisMonth = violationRepository.countByEmployeeIdAndTimestampBetween(
                employeeId, monthStart, now);

        return EmployeeStats.builder()
                .employeeId(employeeId)
                .employeeName(employee.getFullName())
                .totalViolations(totalViolations)
                .violationsThisWeek(violationsThisWeek)
                .violationsThisMonth(violationsThisMonth)
                .build();
    }

    /**
     * Time series data for charts - violations per day over a date range
     */
    @Cacheable(value = "statistics", key = "'timeseries-' + #days")
    public TimeSeriesStats getTimeSeriesStats(int days) {
        logger.info("Calculating time series statistics for {} days (cache miss)", days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<Violation> violations = violationRepository.findByTimestampBetween(start, end);

        // Group by date
        Map<LocalDate, Long> dailyCounts = violations.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getTimestamp().toLocalDate(),
                        Collectors.counting()));

        // Fill in missing dates with 0
        List<TimeSeriesStats.DailyCount> counts = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            counts.add(TimeSeriesStats.DailyCount.builder()
                    .date(date)
                    .count(dailyCounts.getOrDefault(date, 0L))
                    .build());
        }

        // Group by label
        Map<String, List<TimeSeriesStats.DailyCount>> byLabel = new HashMap<>();
        for (Violation v : violations) {
            LocalDate date = v.getTimestamp().toLocalDate();
            for (String label : v.getLabels()) {
                byLabel.computeIfAbsent(label, k -> new ArrayList<>());
            }
        }

        // Calculate daily counts per label
        for (String label : byLabel.keySet()) {
            Map<LocalDate, Long> labelDailyCounts = violations.stream()
                    .filter(v -> v.getLabels().contains(label))
                    .collect(Collectors.groupingBy(
                            v -> v.getTimestamp().toLocalDate(),
                            Collectors.counting()));

            List<TimeSeriesStats.DailyCount> labelCounts = new ArrayList<>();
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                labelCounts.add(TimeSeriesStats.DailyCount.builder()
                        .date(date)
                        .count(labelDailyCounts.getOrDefault(date, 0L))
                        .build());
            }
            byLabel.put(label, labelCounts);
        }

        return TimeSeriesStats.builder()
                .startDate(startDate)
                .endDate(endDate)
                .dailyCounts(counts)
                .byLabel(byLabel)
                .build();
    }

    /**
     * Employee ranking - top and bottom violators
     */
    @Cacheable(value = "statistics", key = "'ranking-' + #limit")
    public EmployeeRanking getEmployeeRanking(int limit) {
        logger.info("Calculating employee ranking with limit {} (cache miss)", limit);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        List<User> employees = userRepository.findByRole(Role.ROLE_EMPLOYEE);
        long totalEmployees = employees.size();

        // Calculate violation counts for all employees
        List<EmployeeRanking.RankedEmployee> allRanked = new ArrayList<>();
        for (User employee : employees) {
            long count = violationRepository.countByEmployeeIdAndTimestampBetween(
                    employee.getId(), monthStart, now);
            allRanked.add(EmployeeRanking.RankedEmployee.builder()
                    .employeeId(employee.getId())
                    .employeeName(employee.getFullName())
                    .email(employee.getEmail())
                    .violationCount(count)
                    .build());
        }

        // Sort by violation count descending
        allRanked.sort((a, b) -> Long.compare(b.getViolationCount(), a.getViolationCount()));

        // Assign ranks
        for (int i = 0; i < allRanked.size(); i++) {
            allRanked.get(i).setRank(i + 1);
        }

        // Get top violators
        List<EmployeeRanking.RankedEmployee> topViolators = allRanked.stream()
                .limit(limit)
                .collect(Collectors.toList());

        // Get least violators (reverse order)
        List<EmployeeRanking.RankedEmployee> leastViolators = allRanked.stream()
                .sorted(Comparator.comparingLong(EmployeeRanking.RankedEmployee::getViolationCount))
                .limit(limit)
                .collect(Collectors.toList());

        // Calculate average
        double average = totalEmployees > 0
                ? allRanked.stream().mapToLong(EmployeeRanking.RankedEmployee::getViolationCount).average().orElse(0)
                : 0;

        return EmployeeRanking.builder()
                .topViolators(topViolators)
                .leastViolators(leastViolators)
                .totalEmployees(totalEmployees)
                .averageViolationsPerEmployee(average)
                .build();
    }

    /**
     * Detailed employee violation report for admin view
     */
    public EmployeeViolationReport getEmployeeViolationReport(Long employeeId) {
        logger.info("Generating detailed report for employee {}", employeeId);

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        EmployeeStats stats = getEmployeeStats(employeeId);

        // Get recent violations (last 10)
        List<Violation> violations = violationRepository.findByEmployeeId(employeeId);
        violations.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        List<ViolationDto> recentViolations = violations.stream()
                .limit(10)
                .map(violationMapper::toDto)
                .collect(Collectors.toList());

        // Calculate most frequent labels
        Map<String, Long> labelCounts = new HashMap<>();
        for (Violation v : violations) {
            for (String label : v.getLabels()) {
                labelCounts.merge(label, 1L, Long::sum);
            }
        }
        List<String> mostFrequentLabels = labelCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return EmployeeViolationReport.builder()
                .employeeId(employeeId)
                .employeeName(employee.getFullName())
                .email(employee.getEmail())
                .stats(stats)
                .recentViolations(recentViolations)
                .mostFrequentLabels(mostFrequentLabels)
                .build();
    }

    private Map<String, Long> getViolationsByLabel(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = violationRepository.findLabelCountByTimeRange(start, end);
        Map<String, Long> labelCounts = new LinkedHashMap<>();

        for (Object[] row : results) {
            String label = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            labelCounts.put(label, count);
        }

        return labelCounts;
    }

    private Map<String, Long> getTopViolators(LocalDateTime start, LocalDateTime end, int limit) {
        List<Object[]> results = violationRepository.findTopViolatorsByTimeRange(start, end, limit);
        Map<String, Long> topViolators = new LinkedHashMap<>();

        for (Object[] row : results) {
            Long employeeId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();

            userRepository.findById(employeeId).ifPresent(user -> topViolators.put(user.getFullName(), count));
        }

        return topViolators;
    }
}
