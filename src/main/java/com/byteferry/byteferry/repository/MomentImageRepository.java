package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.MomentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MomentImageRepository extends JpaRepository<MomentImage, Long> {
    List<MomentImage> findByMomentIdOrderBySortOrder(Long momentId);

    @Query("SELECT mi FROM MomentImage mi WHERE mi.momentId IN :momentIds ORDER BY mi.momentId, mi.sortOrder")
    List<MomentImage> findByMomentIdInOrderBySortOrder(@Param("momentIds") List<Long> momentIds);

    void deleteByMomentId(Long momentId);
}
