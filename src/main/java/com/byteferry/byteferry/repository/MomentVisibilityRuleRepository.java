package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.MomentVisibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MomentVisibilityRuleRepository extends JpaRepository<MomentVisibilityRule, Long> {
    boolean existsByMomentIdAndUserId(Long momentId, Long userId);
    List<MomentVisibilityRule> findByMomentId(Long momentId);
    void deleteByMomentId(Long momentId);
}
