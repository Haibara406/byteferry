package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.MomentImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MomentImageRepository extends JpaRepository<MomentImage, Long> {
    List<MomentImage> findByMomentIdOrderBySortOrder(Long momentId);
    void deleteByMomentId(Long momentId);
}
