package com.pedidai.api.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pages: paginació acotada")
class PagesTest {

    @Test
    @DisplayName("limita la mida a 200 i corregeix valors negatius")
    void clampsPageAndSize() {
        Pageable huge = Pages.of(3, 1_000_000, Sort.unsorted());
        assertThat(huge.getPageSize()).isEqualTo(Pages.MAX_SIZE);
        assertThat(huge.getPageNumber()).isEqualTo(3);

        Pageable negative = Pages.of(-2, 0, Sort.unsorted());
        assertThat(negative.getPageNumber()).isZero();
        assertThat(negative.getPageSize()).isEqualTo(1);
    }
}
