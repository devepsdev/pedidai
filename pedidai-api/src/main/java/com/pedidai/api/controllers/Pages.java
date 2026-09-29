package com.pedidai.api.controllers;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Paginació acotada: pàgina >= 0 i com a màxim {@link #MAX_SIZE} elements per pàgina. */
final class Pages {

    /** L'assistent de comandes demana fins a 200 productes d'un proveïdor. */
    static final int MAX_SIZE = 200;

    private Pages() {
    }

    static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE), sort);
    }
}
