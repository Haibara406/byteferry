package com.byteferry.byteferry.repository;

import com.byteferry.byteferry.model.entity.MomentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MomentTemplateRepository extends JpaRepository<MomentTemplate, String> {
    List<MomentTemplate> findAllByOrderBySortOrder();
}
