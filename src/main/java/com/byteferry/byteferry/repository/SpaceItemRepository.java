package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.SpaceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpaceItemRepository extends JpaRepository<SpaceItem, Long> {
    List<SpaceItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT i FROM SpaceItem i WHERE i.userId = :userId AND (i.expireAt IS NULL OR i.expireAt > :now) ORDER BY i.createdAt DESC")
    List<SpaceItem> findActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(i) FROM SpaceItem i WHERE i.userId = :userId AND (i.expireAt IS NULL OR i.expireAt > :now)")
    long countActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    List<SpaceItem> findByExpireAtNotNullAndExpireAtBefore(LocalDateTime now);

    List<SpaceItem> findByUserId(Long userId);
}
