package com.pedidai.api.services;

import com.pedidai.api.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

public interface OrderService {

    OrderResponseDTO createOrder(OrderRequestDTO orderRequestDTO);

    Page<OrderResponseDTO> filterOrders(OrderFilterDTO dto, Pageable pageable);

    OrderResponseDTO getOrderByUuid(String uuid);

    OrderResponseDTO deleteOrder(String uuid);

    OrderResponseDTO updateOrder(String uuid, OrderRequestDTO dto);

    OrderResponseDTO sendOrder(String orderUuid);

    ConsumptionAnalysisDTO getConsumptionAnalysis(int days);
}
