package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.MomentReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MomentReadStatusRepository extends JpaRepository<MomentReadStatus, Long> {
    Optional<MomentReadStatus> findByUserId(Long userId);
}
