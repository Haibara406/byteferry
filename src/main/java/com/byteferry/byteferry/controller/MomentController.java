package com.byteferry.byteferry.controller;

import com.byteferry.byteferry.model.entity.Moment;
import com.byteferry.byteferry.model.entity.MomentShareLink;
import com.byteferry.byteferry.model.entity.MomentTemplate;
import com.byteferry.byteferry.model.enums.Visibility;
import com.byteferry.byteferry.service.MomentService;
import com.byteferry.byteferry.service.MomentTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/moment")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;
    private final MomentTemplateService momentTemplateService;

    @PostMapping
    public ResponseEntity<Moment> createMoment(
            Authentication auth,
            @RequestParam(required = false) String textContent,
            @RequestParam(required = false) String htmlContent,
            @RequestParam(required = false) String templateId,
            @RequestParam(defaultValue = "PUBLIC") String visibility,
            @RequestParam(required = false) String visibleUserIds,
            @RequestParam(required = false) MultipartFile[] images,
            @RequestParam(required = false) MultipartFile[] liveImages,
            @RequestParam(required = false) MultipartFile[] liveVideos) {

        Long userId = (Long) auth.getPrincipal();

        List<Long> userIds = null;
        if (visibleUserIds != null && !visibleUserIds.isBlank()) {
            userIds = Arrays.stream(visibleUserIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }

        Moment moment = momentService.createMoment(
                userId,
                textContent,
                htmlContent,
                templateId,
                Visibility.valueOf(visibility.toUpperCase()),
                userIds,
                images,
                liveImages,
                liveVideos
        );

        return ResponseEntity.ok(moment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Moment> getMoment(@PathVariable Long id, Authentication auth) {
        Long viewerId = (Long) auth.getPrincipal();
        Moment moment = momentService.getMoment(id, viewerId);
        return ResponseEntity.ok(moment);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<Moment>> getMyMoments(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = (Long) auth.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<Moment> moments = momentService.getMyMoments(userId, pageable);
        return ResponseEntity.ok(moments);
    }

    @GetMapping("/timeline")
    public ResponseEntity<Page<Moment>> getTimeline(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long viewerId = (Long) auth.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<Moment> moments = momentService.getAllPublicMoments(viewerId, pageable);
        return ResponseEntity.ok(moments);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<Page<Moment>> getUserMoments(
            @PathVariable String username,
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long viewerId = (Long) auth.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<Moment> moments = momentService.getUserMoments(username, viewerId, pageable);
        return ResponseEntity.ok(moments);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Moment> updateMoment(
            @PathVariable Long id,
            Authentication auth,
            @RequestParam(required = false) String textContent,
            @RequestParam(required = false) String htmlContent,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String visibleUserIds) {

        Long userId = (Long) auth.getPrincipal();

        List<Long> userIds = null;
        if (visibleUserIds != null && !visibleUserIds.isBlank()) {
            userIds = Arrays.stream(visibleUserIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }

        Visibility vis = visibility != null ? Visibility.valueOf(visibility.toUpperCase()) : null;

        Moment moment = momentService.updateMoment(id, userId, textContent, htmlContent, vis, userIds);
        return ResponseEntity.ok(moment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMoment(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        momentService.deleteMoment(id, userId);
        return ResponseEntity.ok().build();
    }

    // Template endpoints
    @GetMapping("/templates")
    public ResponseEntity<List<MomentTemplate>> getAllTemplates() {
        return ResponseEntity.ok(momentTemplateService.getAllTemplates());
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<MomentTemplate> getTemplate(@PathVariable String id) {
        return ResponseEntity.ok(momentTemplateService.getTemplate(id));
    }

    // Part 6: Share Link endpoints
    @PostMapping("/share/generate")
    public ResponseEntity<MomentShareLink> generateShareLink(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        MomentShareLink shareLink = momentService.generateShareLink(userId);
        return ResponseEntity.ok(shareLink);
    }

    @GetMapping("/share/my")
    public ResponseEntity<MomentShareLink> getMyShareLink(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        MomentShareLink shareLink = momentService.getMyShareLink(userId);
        return ResponseEntity.ok(shareLink);
    }

    @GetMapping("/share/{shareCode}")
    public ResponseEntity<Page<Moment>> getMomentsByShareCode(
            @PathVariable String shareCode,
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long viewerId = (Long) auth.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<Moment> moments = momentService.getMomentsByShareCode(shareCode, viewerId, pageable);
        return ResponseEntity.ok(moments);
    }
}
