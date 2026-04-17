package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponseDTO<T> {

    private List<T> content;

    private PageableInfo pageable;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageableInfo {

        // ===== INFORMACIÓ DE LA PÀGINA ACTUAL =====
        private int page;

        private int size;

        private String sort;

        // ===== ESTADÍSTIQUES GENERALS =====
        private int totalPages;

        private long totalElements;

        private int numberOfElements;

        private boolean first;

        private boolean last;

        private boolean empty;
    }

    public static <T> PagedResponseDTO<T> of(Page<T> page) {
        PageableInfo pageableInfo = PageableInfo.builder()
                .page(page.getPageable().getPageNumber())
                .size(page.getPageable().getPageSize())
                .sort(formatSort(page.getPageable().getSort()))
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .numberOfElements(page.getNumberOfElements())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();

        return PagedResponseDTO.<T>builder()
                .content(page.getContent())
                .pageable(pageableInfo)
                .build();
    }

    private static String formatSort(Sort sort) {
        if (sort.isEmpty()) {
            return "unsorted";
        }

        return sort.stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .reduce((a, b) -> a + ";" + b)
                .orElse("unsorted");
    }
}