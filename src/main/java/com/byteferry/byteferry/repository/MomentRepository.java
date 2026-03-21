package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentRepository extends JpaRepository<Moment, Long> {
    Page<Moment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Moment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
