package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.services.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> createOrder(
            @Valid @RequestBody OrderRequestDTO orderRequestDTO){

        OrderResponseDTO createOrder = orderService.createOrder(orderRequestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(createOrder, "Comanda creada correctament"));
    }

    @GetMapping({"/filter", "/list"})
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<OrderResponseDTO>>> filterOrders(@Valid OrderFilterDTO filterDTO){

        // Ordenació
        Sort sort = filterDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(filterDTO.getSortBy()).descending() :
                Sort.by(filterDTO.getSortBy()).ascending();

        // Paginació
        Pageable pageable = PageRequest.of(filterDTO.getPage(), filterDTO.getSize(), sort);
        Page<OrderResponseDTO> orders = orderService.filterOrders(filterDTO, pageable);
        PagedResponseDTO<OrderResponseDTO> pagedResponse = PagedResponseDTO.of(orders);

        // Retorn
        String message = String.format("Cerca avançada de comandes completada. Filtres aplicats: text=%s", filterDTO.hasTextFilters());
        return ResponseEntity.ok(ApiResponseDTO.success(pagedResponse, message));
    }

    @PostMapping("/{uuid}/send")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> sendOrder(
            @PathVariable String uuid) {

        OrderResponseDTO sentOrder = orderService.sendOrder(uuid);
        return ResponseEntity
                .ok(ApiResponseDTO.success(sentOrder, "Comanda enviada correctament"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> getOrder(
            @PathVariable @NotBlank(message = "L'UUID no pot estar buit") String uuid) {
        OrderResponseDTO order = orderService.getOrderByUuid(uuid);
        return ResponseEntity.ok(
                ApiResponseDTO.success(order, "Comanda trobada correctament"));
    }

    @PatchMapping("/delete/{uuid}")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> deleteOrder(
            @PathVariable @NotBlank(message = "L'UUID no pot estar buit") String uuid) {
        OrderResponseDTO deletedOrder = orderService.deleteOrder(uuid);
        return ResponseEntity.ok(
                ApiResponseDTO.success(deletedOrder, "Comanda eliminada correctament"));
    }

    @PutMapping("/update/{uuid}")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> updateOrder(
            @PathVariable @NotBlank String uuid,
            @Valid @RequestBody OrderRequestDTO dto) {

        OrderResponseDTO updatedOrder = orderService.updateOrder(uuid, dto);
        return ResponseEntity.ok(
                ApiResponseDTO.success(updatedOrder, "Comanda actualitzada correctament")
        );
    }

    @GetMapping("/consumption-analysis")
    public ResponseEntity<ApiResponseDTO<ConsumptionAnalysisDTO>> getConsumptionAnalysis(
            @RequestParam(defaultValue = "90") int days) {

        ConsumptionAnalysisDTO analysis = orderService.getConsumptionAnalysis(days);
        return ResponseEntity.ok(
                ApiResponseDTO.success(analysis, "Anàlisi de consum generat correctament")
        );
    }


}