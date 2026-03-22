package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MomentRepository extends JpaRepository<Moment, Long> {
    Page<Moment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Moment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(m) FROM Moment m WHERE m.userId != :userId AND m.createdAt > :lastReadAt")
    long countUnreadMoments(@Param("userId") Long userId, @Param("lastReadAt") LocalDateTime lastReadAt);

    @Query("SELECT COUNT(m) FROM Moment m WHERE m.userId != :userId")
    long countByUserIdNot(@Param("userId") Long userId);
}
