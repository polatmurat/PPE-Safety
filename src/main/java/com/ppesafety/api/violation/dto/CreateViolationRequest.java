package com.ppesafety.api.violation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateViolationRequest {

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    @NotEmpty(message = "At least one label is required")
    private List<String> labels;

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    private String location;

    private LocalDateTime timestamp;
}
