package com.ppesafety.api.violation.repository;

import com.ppesafety.api.violation.entity.Violation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    List<Violation> findByEmployeeId(Long employeeId);

    Page<Violation> findByEmployeeId(Long employeeId, Pageable pageable);

    List<Violation> findByReportedById(Long reportedById);

    List<Violation> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT v FROM Violation v WHERE v.employee.id = :employeeId AND v.timestamp BETWEEN :start AND :end")
    List<Violation> findByEmployeeIdAndTimestampBetween(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(v) FROM Violation v WHERE v.timestamp BETWEEN :start AND :end")
    long countByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(v) FROM Violation v WHERE v.employee.id = :employeeId")
    long countByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT COUNT(v) FROM Violation v WHERE v.employee.id = :employeeId AND v.timestamp BETWEEN :start AND :end")
    long countByEmployeeIdAndTimestampBetween(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query(value = "SELECT v.employee_id, COUNT(*) as violation_count FROM violations v " +
            "WHERE v.timestamp BETWEEN :start AND :end " +
            "GROUP BY v.employee_id ORDER BY violation_count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopViolatorsByTimeRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit);

    @Query(value = "SELECT label, COUNT(*) as label_count FROM violations v, " +
            "jsonb_array_elements_text(v.labels) as label " +
            "WHERE v.timestamp BETWEEN :start AND :end " +
            "GROUP BY label ORDER BY label_count DESC", nativeQuery = true)
    List<Object[]> findLabelCountByTimeRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
