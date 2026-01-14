package com.ppesafety.api.violation.service;

import com.ppesafety.api.core.annotation.Auditable;
import com.ppesafety.api.core.exception.BadRequestException;
import com.ppesafety.api.core.exception.ResourceNotFoundException;
import com.ppesafety.api.user.entity.Role;
import com.ppesafety.api.user.entity.User;
import com.ppesafety.api.user.repository.UserRepository;
import com.ppesafety.api.violation.dto.CreateViolationRequest;
import com.ppesafety.api.violation.dto.ViolationDto;
import com.ppesafety.api.violation.entity.Violation;
import com.ppesafety.api.violation.mapper.ViolationMapper;
import com.ppesafety.api.violation.repository.ViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ViolationService {

    private static final Logger logger = LoggerFactory.getLogger(ViolationService.class);

    // Allowed labels for violations
    public static final Set<String> ALLOWED_LABELS = new HashSet<>(
            Arrays.asList("Helmet", "Vest", "Head", "Person", "No Helmet", "No Vest"));

    private final ViolationRepository violationRepository;
    private final UserRepository userRepository;
    private final ViolationMapper violationMapper;

    public ViolationService(ViolationRepository violationRepository,
            UserRepository userRepository,
            ViolationMapper violationMapper) {
        this.violationRepository = violationRepository;
        this.userRepository = userRepository;
        this.violationMapper = violationMapper;
    }

    @Transactional(readOnly = true)
    public List<ViolationDto> getAllViolations() {
        return violationMapper.toDtoList(violationRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ViolationDto getViolationById(Long id) {
        Violation violation = violationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Violation", id));
        return violationMapper.toDto(violation);
    }

    @Transactional(readOnly = true)
    public List<ViolationDto> getViolationsByEmployeeId(Long employeeId) {
        return violationMapper.toDtoList(violationRepository.findByEmployeeId(employeeId));
    }

    @Transactional(readOnly = true)
    public Page<ViolationDto> getViolationsByEmployeeId(Long employeeId, Pageable pageable) {
        return violationRepository.findByEmployeeId(employeeId, pageable)
                .map(violationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<ViolationDto> getViolationsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return violationMapper.toDtoList(violationRepository.findByTimestampBetween(start, end));
    }

    @Auditable(action = "CREATE_VIOLATION")
    @Caching(evict = {
            @CacheEvict(value = "statistics", allEntries = true),
            @CacheEvict(value = "violations", allEntries = true)
    })
    public ViolationDto createViolation(CreateViolationRequest request, User reportedBy) {
        // Validate labels
        for (String label : request.getLabels()) {
            if (!ALLOWED_LABELS.contains(label)) {
                throw new BadRequestException("Invalid label: " + label + ". Allowed: " + ALLOWED_LABELS);
            }
        }

        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));

        // Validate that the employee has EMPLOYEE role
        if (employee.getRole() != Role.ROLE_EMPLOYEE) {
            throw new BadRequestException("User with ID " + request.getEmployeeId() + " is not an employee");
        }

        Violation violation = Violation.builder()
                .imageUrl(request.getImageUrl())
                .labels(request.getLabels())
                .employee(employee)
                .reportedBy(reportedBy)
                .location(request.getLocation())
                .timestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now())
                .build();

        Violation saved = violationRepository.save(violation);
        logger.info("Created violation {} for employee {} by {}",
                saved.getId(), employee.getUsername(), reportedBy.getUsername());

        return violationMapper.toDto(saved);
    }

    @Auditable(action = "DELETE_VIOLATION")
    @Caching(evict = {
            @CacheEvict(value = "statistics", allEntries = true),
            @CacheEvict(value = "violations", allEntries = true)
    })
    public void deleteViolation(Long id) {
        if (!violationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Violation", id);
        }
        violationRepository.deleteById(id);
        logger.info("Deleted violation with id: {}", id);
    }
}
