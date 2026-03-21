package com.byteferry.byteferry.service;

import com.byteferry.byteferry.model.entity.MomentTemplate;
import com.byteferry.byteferry.repository.MomentTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MomentTemplateService {

    private final MomentTemplateRepository momentTemplateRepository;

    public List<MomentTemplate> getAllTemplates() {
        return momentTemplateRepository.findAllByOrderBySortOrder();
    }

    public MomentTemplate getTemplate(String id) {
        return momentTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }
}
