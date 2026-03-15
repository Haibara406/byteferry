package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.SpaceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceItemRepository extends JpaRepository<SpaceItem, Long> {
    List<SpaceItem> findByUserIdOrderByCreatedAtDesc(Long userId);
}
