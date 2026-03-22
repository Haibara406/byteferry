package com.byteferry.byteferry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

/**
 * 可序列化的分页结果包装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    public Page<T> toPage() {
        return new org.springframework.data.domain.PageImpl<>(
                content,
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize),
                totalElements
        );
    }
}
