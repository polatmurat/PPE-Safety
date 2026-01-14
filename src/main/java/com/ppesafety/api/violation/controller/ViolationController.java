package com.ppesafety.api.violation.controller;

import com.ppesafety.api.core.dto.ApiResponse;
import com.ppesafety.api.core.service.FileStorageService;
import com.ppesafety.api.user.entity.User;
import com.ppesafety.api.violation.dto.CreateViolationRequest;
import com.ppesafety.api.violation.dto.ViolationDto;
import com.ppesafety.api.violation.service.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/violations")
@Tag(name = "Violations", description = "Violation recording and management")
@SecurityRequirement(name = "Bearer Authentication")
public class ViolationController {

        private final ViolationService violationService;
        private final FileStorageService fileStorageService;

        public ViolationController(ViolationService violationService, FileStorageService fileStorageService) {
                this.violationService = violationService;
                this.fileStorageService = fileStorageService;
        }

        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'SAFETY_SPECIALIST')")
        @Operation(summary = "Get all violations", description = "Retrieves all violations. Admin and Safety Specialist only.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Violations retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<ApiResponse<List<ViolationDto>>> getAllViolations() {
                List<ViolationDto> violations = violationService.getAllViolations();
                return ResponseEntity.ok(ApiResponse.success(violations));
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get violation by ID", description = "Retrieves a specific violation")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Violation retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Violation not found")
        })
        public ResponseEntity<ApiResponse<ViolationDto>> getViolationById(@PathVariable Long id) {
                ViolationDto violation = violationService.getViolationById(id);
                return ResponseEntity.ok(ApiResponse.success(violation));
        }

        @GetMapping("/employee/{employeeId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'SAFETY_SPECIALIST') or #employeeId == authentication.principal.id")
        @Operation(summary = "Get violations by employee", description = "Retrieves violations for a specific employee")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Violations retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<ApiResponse<List<ViolationDto>>> getViolationsByEmployee(
                        @PathVariable Long employeeId) {
                List<ViolationDto> violations = violationService.getViolationsByEmployeeId(employeeId);
                return ResponseEntity.ok(ApiResponse.success(violations));
        }

        @GetMapping("/my")
        @Operation(summary = "Get my violations", description = "Retrieves violations for the current user")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Violations retrieved successfully")
        })
        public ResponseEntity<ApiResponse<List<ViolationDto>>> getMyViolations(
                        @AuthenticationPrincipal User currentUser) {
                List<ViolationDto> violations = violationService.getViolationsByEmployeeId(currentUser.getId());
                return ResponseEntity.ok(ApiResponse.success(violations));
        }

        @GetMapping("/date-range")
        @PreAuthorize("hasAnyRole('ADMIN', 'SAFETY_SPECIALIST')")
        @Operation(summary = "Get violations by date range", description = "Retrieves violations within a date range")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Violations retrieved successfully")
        })
        public ResponseEntity<ApiResponse<List<ViolationDto>>> getViolationsByDateRange(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
                List<ViolationDto> violations = violationService.getViolationsByTimeRange(start, end);
                return ResponseEntity.ok(ApiResponse.success(violations));
        }

        // ========== NEW: Create violation with file upload ==========
        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyRole('ADMIN', 'SAFETY_SPECIALIST')")
        @Operation(summary = "Create violation with image upload", description = "Records a new violation with image file. Admin and Safety Specialist only.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Violation created successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<ApiResponse<ViolationDto>> createViolationWithFile(
                        @RequestParam("image") MultipartFile image,
                        @RequestParam("labels") List<String> labels,
                        @RequestParam("employeeId") Long employeeId,
                        @RequestParam(value = "location", required = false) String location,
                        @RequestParam(value = "timestamp", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp,
                        @AuthenticationPrincipal User currentUser) {

                // Store file and get filename
                String filename = fileStorageService.storeFile(image);
                String imageUrl = "/uploads/" + filename;

                // Create request
                CreateViolationRequest request = CreateViolationRequest.builder()
                                .imageUrl(imageUrl)
                                .labels(labels)
                                .employeeId(employeeId)
                                .location(location)
                                .timestamp(timestamp != null ? timestamp : LocalDateTime.now())
                                .build();

                ViolationDto violation = violationService.createViolation(request, currentUser);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(violation, "Violation recorded successfully"));
        }

        // Keep old JSON endpoint for backwards compatibility
        @PostMapping(path = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        @PreAuthorize("hasAnyRole('ADMIN', 'SAFETY_SPECIALIST')")
        @Operation(summary = "Create violation (JSON)", description = "Records a new violation with imageUrl. Legacy endpoint.")
        public ResponseEntity<ApiResponse<ViolationDto>> createViolationJson(
                        @Valid @RequestBody CreateViolationRequest request,
                        @AuthenticationPrincipal User currentUser) {
                ViolationDto violation = violationService.createViolation(request, currentUser);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(violation, "Violation recorded successfully"));
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Delete violation", description = "Deletes a violation. Admin only.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Violation deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Violation not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<Void> deleteViolation(@PathVariable Long id) {
                violationService.deleteViolation(id);
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/labels")
        @Operation(summary = "Get allowed labels", description = "Returns the list of allowed violation labels")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Labels retrieved successfully")
        })
        public ResponseEntity<ApiResponse<Set<String>>> getAllowedLabels() {
                return ResponseEntity.ok(ApiResponse.success(ViolationService.ALLOWED_LABELS));
        }
}
