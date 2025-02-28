package com.stress.repository;

import com.stress.entity.StressResult;
import com.stress.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StressResultRepository extends JpaRepository<StressResult, Long> {
	List<StressResult> findByUserOrderByCreatedAtDesc(User user);
    List<StressResult> findByUserId(Long userId);
       List<StressResult> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, LocalDateTime startDate, LocalDateTime endDate);
}