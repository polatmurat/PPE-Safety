package com.ppesafety.api.violation.dto;

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
public class ViolationDto {

    private Long id;
    private String imageUrl;
    private List<String> labels;
    private Long employeeId;
    private String employeeName;
    private Long reportedById;
    private String reportedByName;
    private String location;
    private LocalDateTime timestamp;
}
