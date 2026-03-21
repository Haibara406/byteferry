package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.MomentShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MomentShareLinkRepository extends JpaRepository<MomentShareLink, Long> {
    Optional<MomentShareLink> findByUserId(Long userId);
    Optional<MomentShareLink> findByShareCode(String shareCode);
}
