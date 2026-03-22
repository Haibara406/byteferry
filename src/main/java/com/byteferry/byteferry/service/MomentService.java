package com.byteferry.byteferry.service;

import com.byteferry.byteferry.enums.UploadEnum;
import com.byteferry.byteferry.model.entity.*;
import com.byteferry.byteferry.model.enums.Visibility;
import com.byteferry.byteferry.repository.*;
import com.byteferry.byteferry.util.FileUploadUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MomentService {

    private final MomentRepository momentRepository;
    private final MomentImageRepository momentImageRepository;
    private final MomentVisibilityRuleRepository visibilityRuleRepository;
    private final MomentShareLinkRepository shareLinkRepository;
    private final MomentTemplateRepository momentTemplateRepository;
    private final UserRepository userRepository;
    private final FileUploadUtils fileUploadUtils;

    @Transactional
    public Moment createMoment(Long userId, String textContent, String htmlContent, String templateId,
                                Visibility visibility, List<Long> visibleUserIds,
                                MultipartFile[] images, MultipartFile[] liveImages, MultipartFile[] liveVideos) {

        // Apply template if specified
        if (templateId != null && !templateId.isBlank() && textContent != null) {
            MomentTemplate template = momentTemplateRepository.findById(templateId).orElse(null);
            if (template != null) {
                htmlContent = template.getHtmlTemplate()
                        .replace("{{content}}", textContent)
                        .replace("{{title}}", textContent.split("\n")[0]); // First line as title
            }
        }

        // Create moment
        Moment moment = Moment.builder()
                .userId(userId)
                .textContent(textContent)
                .htmlContent(htmlContent)
                .templateId(templateId)
                .visibility(visibility)
                .build();

        moment = momentRepository.save(moment);

        // Handle visibility rules
        if ((visibility == Visibility.VISIBLE_TO || visibility == Visibility.HIDDEN_FROM) && visibleUserIds != null) {
            for (Long targetUserId : visibleUserIds) {
                MomentVisibilityRule rule = MomentVisibilityRule.builder()
                        .momentId(moment.getId())
                        .userId(targetUserId)
                        .build();
                visibilityRuleRepository.save(rule);
            }
        }

        // Handle regular images
        if (images != null && images.length > 0) {
            for (int i = 0; i < Math.min(images.length, 9); i++) {
                String imageUrl = fileUploadUtils.upload(UploadEnum.MOMENT_IMAGE, images[i]);
                MomentImage momentImage = MomentImage.builder()
                        .momentId(moment.getId())
                        .imageUrl(imageUrl)
                        .isLivePhoto(false)
                        .sortOrder(i)
                        .build();
                momentImageRepository.save(momentImage);
            }
        }

        // Handle live photos
        if (liveImages != null && liveVideos != null && liveImages.length == liveVideos.length) {
            int offset = (images != null) ? images.length : 0;
            for (int i = 0; i < Math.min(liveImages.length, 9 - offset); i++) {
                String imageUrl = fileUploadUtils.upload(UploadEnum.MOMENT_IMAGE, liveImages[i]);
                String videoUrl = fileUploadUtils.upload(UploadEnum.MOMENT_VIDEO, liveVideos[i]);
                MomentImage momentImage = MomentImage.builder()
                        .momentId(moment.getId())
                        .imageUrl(imageUrl)
                        .videoUrl(videoUrl)
                        .isLivePhoto(true)
                        .sortOrder(offset + i)
                        .build();
                momentImageRepository.save(momentImage);
            }
        }

        // Reload moment with images
        Moment result = momentRepository.findById(moment.getId()).orElse(moment);
        result.setImages(momentImageRepository.findByMomentIdOrderBySortOrder(moment.getId()));
        loadUserInfo(result);
        return result;
    }

    public Moment getMoment(Long id, Long viewerId) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Moment not found"));

        if (!canView(moment, viewerId)) {
            throw new RuntimeException("No permission to view this moment");
        }

        // Load images and user info
        moment.setImages(momentImageRepository.findByMomentIdOrderBySortOrder(id));
        loadUserInfo(moment);
        return moment;
    }

    public Page<Moment> getMyMoments(Long userId, Pageable pageable) {
        Page<Moment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        moments.forEach(m -> {
            m.setImages(momentImageRepository.findByMomentIdOrderBySortOrder(m.getId()));
            loadUserInfo(m);
        });
        return moments;
    }

    public Page<Moment> getUserMoments(String username, Long viewerId, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<Moment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        // Filter by visibility and load images
        moments.forEach(moment -> {
            if (canView(moment, viewerId)) {
                moment.setImages(momentImageRepository.findByMomentIdOrderBySortOrder(moment.getId()));
                loadUserInfo(moment);
            }
        });

        return moments;
    }

    public Page<Moment> getAllPublicMoments(Long viewerId, Pageable pageable) {
        Page<Moment> moments = momentRepository.findAllByOrderByCreatedAtDesc(pageable);

        // Filter by visibility and load images
        moments.forEach(moment -> {
            if (canView(moment, viewerId)) {
                moment.setImages(momentImageRepository.findByMomentIdOrderBySortOrder(moment.getId()));
                loadUserInfo(moment);
            }
        });

        return moments;
    }

    private void loadUserInfo(Moment moment) {
        userRepository.findById(moment.getUserId()).ifPresent(user -> {
            moment.setUsername(user.getUsername());
            moment.setAvatar(user.getAvatar());
        });
    }

    @Transactional
    public Moment updateMoment(Long id, Long userId, String textContent, String htmlContent,
                                Visibility visibility, List<Long> visibleUserIds) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Moment not found"));

        if (!moment.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to edit this moment");
        }

        if (textContent != null) moment.setTextContent(textContent);
        if (htmlContent != null) moment.setHtmlContent(htmlContent);
        if (visibility != null) {
            moment.setVisibility(visibility);

            // Update visibility rules
            visibilityRuleRepository.deleteByMomentId(id);
            if ((visibility == Visibility.VISIBLE_TO || visibility == Visibility.HIDDEN_FROM) && visibleUserIds != null) {
                for (Long targetUserId : visibleUserIds) {
                    MomentVisibilityRule rule = MomentVisibilityRule.builder()
                            .momentId(id)
                            .userId(targetUserId)
                            .build();
                    visibilityRuleRepository.save(rule);
                }
            }
        }

        return momentRepository.save(moment);
    }

    @Transactional
    public void deleteMoment(Long id, Long userId) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Moment not found"));

        if (!moment.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to delete this moment");
        }

        // Delete files from MinIO
        List<MomentImage> images = momentImageRepository.findByMomentIdOrderBySortOrder(id);
        List<String> urls = new ArrayList<>();
        for (MomentImage image : images) {
            urls.add(image.getImageUrl());
            if (image.getVideoUrl() != null) {
                urls.add(image.getVideoUrl());
            }
        }
        if (!urls.isEmpty()) {
            fileUploadUtils.deleteByUrls(urls);
        }

        momentRepository.deleteById(id);
    }

    public boolean canView(Moment moment, Long viewerId) {
        if (moment.getUserId().equals(viewerId)) return true;

        switch (moment.getVisibility()) {
            case PUBLIC:
                return true;
            case PRIVATE:
                return false;
            case VISIBLE_TO:
                return visibilityRuleRepository.existsByMomentIdAndUserId(moment.getId(), viewerId);
            case HIDDEN_FROM:
                return !visibilityRuleRepository.existsByMomentIdAndUserId(moment.getId(), viewerId);
            default:
                return false;
        }
    }

    // Part 6: Share Link methods
    @Transactional
    public MomentShareLink generateShareLink(Long userId) {
        MomentShareLink existing = shareLinkRepository.findByUserId(userId).orElse(null);

        if (existing != null) {
            // Regenerate share code
            existing.setShareCode(UUID.randomUUID().toString().replace("-", ""));
            return shareLinkRepository.save(existing);
        } else {
            MomentShareLink shareLink = MomentShareLink.builder()
                    .userId(userId)
                    .shareCode(UUID.randomUUID().toString().replace("-", ""))
                    .build();
            return shareLinkRepository.save(shareLink);
        }
    }

    public MomentShareLink getMyShareLink(Long userId) {
        return shareLinkRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Share link not found. Please generate one first."));
    }

    public Page<Moment> getMomentsByShareCode(String shareCode, Long viewerId, Pageable pageable) {
        MomentShareLink shareLink = shareLinkRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new RuntimeException("Invalid share code"));

        return getUserMoments(
                userRepository.findById(shareLink.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"))
                        .getUsername(),
                viewerId,
                pageable
        );
    }
}
