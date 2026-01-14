package com.ppesafety.api.statistics.controller;

import com.ppesafety.api.core.dto.ApiResponse;
import com.ppesafety.api.statistics.dto.*;
import com.ppesafety.api.statistics.service.StatisticsService;
import com.ppesafety.api.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Statistics", description = "Dashboard and reporting endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SAFETY_SPECIALIST')")
    @Operation(summary = "Get dashboard statistics", description = "Returns overall violation statistics. Admin and Safety Specialist only. Results are cached.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<DashboardStats>> getDashboardStats() {
        DashboardStats stats = statisticsService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SAFETY_SPECIALIST') or #employeeId == authentication.principal.id")
    @Operation(summary = "Get employee statistics", description = "Returns violation statistics for a specific employee. Employees can only view their own stats.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<EmployeeStats>> getEmployeeStats(@PathVariable Long employeeId) {
        EmployeeStats stats = statisticsService.getEmployeeStats(employeeId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my statistics", description = "Returns violation statistics for the currently authenticated user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public ResponseEntity<ApiResponse<EmployeeStats>> getMyStats(
            @AuthenticationPrincipal User currentUser) {
        EmployeeStats stats = statisticsService.getEmployeeStats(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ========== NEW ADMIN DASHBOARD ENDPOINTS ==========

    @GetMapping("/time-series")
    @PreAuthorize("hasAnyRole('ADMIN', 'SAFETY_SPECIALIST')")
    @Operation(summary = "Get time series statistics", description = "Returns daily violation counts for charts. Use for trend visualization.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Time series data retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<TimeSeriesStats>> getTimeSeriesStats(
            @Parameter(description = "Number of days to include (default: 30)") @RequestParam(defaultValue = "30") int days) {
        TimeSeriesStats stats = statisticsService.getTimeSeriesStats(days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMIN', 'SAFETY_SPECIALIST')")
    @Operation(summary = "Get employee ranking", description = "Returns top and bottom violators ranking. Use for leaderboards.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ranking retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<EmployeeRanking>> getEmployeeRanking(
            @Parameter(description = "Number of employees to include in top/bottom lists (default: 10)") @RequestParam(defaultValue = "10") int limit) {
        EmployeeRanking ranking = statisticsService.getEmployeeRanking(limit);
        return ResponseEntity.ok(ApiResponse.success(ranking));
    }

    @GetMapping("/employee/{employeeId}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'SAFETY_SPECIALIST')")
    @Operation(summary = "Get detailed employee violation report", description = "Returns comprehensive violation report for an employee including recent violations and frequent labels.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<EmployeeViolationReport>> getEmployeeViolationReport(
            @PathVariable Long employeeId) {
        EmployeeViolationReport report = statisticsService.getEmployeeViolationReport(employeeId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
