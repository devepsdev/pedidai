package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.*;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.*;
import com.pedidai.api.services.NotificationService;
import com.pedidai.api.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO orderRequestDTO) {

        // Crear la instància de l'entitat Order
        Order order = new Order();
        order.setUuid(UUID.randomUUID().toString());
        order.setName(orderRequestDTO.getName());
        order.setNotes(orderRequestDTO.getNotes());
        order.setDeliveryDate(orderRequestDTO.getDeliveryDate());
        order.setStatus(Order.OrderStatus.PENDING);

        // Assignar company i usuari
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));

        order.setUser(user);
        order.setCompany(user.getCompany());

        // Buscar el proveïdor pel UUID
        Supplier supplier = supplierRepository.findByUuid(orderRequestDTO.getSupplierUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Proveïdor no trobat"));
        order.setSupplier(supplier);

        // Crear els items associats a la comanda
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequestDTO itemDTO : orderRequestDTO.getItems()) {
            OrderItem item = new OrderItem();
            item.setUuid(UUID.randomUUID().toString());
            item.setOrder(order);

            Product product = productRepository.findByUuid(itemDTO.getProductUuid())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producte no trobat: " + itemDTO.getProductUuid()));

            item.setProduct(product);
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(item.getQuantity().multiply(item.getUnitPrice()));
            item.setNotes(itemDTO.getNotes());

            totalAmount = totalAmount.add(item.getSubtotal());
            orderItems.add(item);
        }

        // Assignar el total i guardar
        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);

        log.info("Comanda {} creada correctament per l'usuari {} amb estat PENDING",
                order.getUuid(), username);

        return buildOrderResponseDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> filterOrders(OrderFilterDTO dto, Pageable pageable){

        // Usuari autenticat
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username).orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));
        Long companyId = user.getCompany().getId();
        Long userId = null;
        Long supplierId = null;
        Long orderId = null;

        // Si rebem comanda
        if (dto.getOrderUuid() != null && !dto.getOrderUuid().isBlank()) {
            Order order = orderRepository.findByUuid(dto.getOrderUuid()).orElseThrow(() -> new ResourceNotFoundException("Comanda no trobada"));
            orderId = order.getId();
        }

        // Si rebem proveïdor
        if (dto.getSupplierUuid() != null && !dto.getSupplierUuid().isBlank()) {
            Supplier supplier = supplierRepository.findByUuid(dto.getSupplierUuid()).orElseThrow(() -> new ResourceNotFoundException("Proveïdor no trobat"));
            supplierId = supplier.getId();
        }

        // Si rebem usuari
        if (dto.getUserUuid() != null && !dto.getUserUuid().isBlank()) {
            user = userRepository.findByUuid(dto.getUserUuid()).orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat"));
            userId = user.getId();
        }

        // Convertir estat (string → enum)
        Order.OrderStatus orderStatus = null;
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            try {
                orderStatus = Order.OrderStatus.valueOf(dto.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Estat no vàlid: " + dto.getStatus());
            }
        }

        // Crear la Specification
        var spec = OrderSpecifications.filterOrders(
                orderId,
                companyId,
                supplierId,
                userId,
                dto.getSearchText(),
                dto.getName(),
                dto.getNotes(),
                orderStatus,
                dto.getMinAmount(),
                dto.getMaxAmount(),
                dto.getDeliveryDateFrom(),
                dto.getDeliveryDateTo(),
                dto.getCreatedAtFrom(),
                dto.getCreatedAtTo(),
                dto.getUpdatedAtFrom(),
                dto.getUpdatedAtTo()
        );

        // llistat des de orderRepository
        Page<Order> orders = orderRepository.findAll(spec, pageable);

        // Retorn
        return orders.map(this::mapToResponseDTO);
    }

    @Override
    @Transactional
    public OrderResponseDTO sendOrder(String orderUuid) {
        log.info("Intentant enviar la comanda {}", orderUuid);

        // Buscar la comanda
        Order order = orderRepository.findByUuid(orderUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comanda no trobada: " + orderUuid));

        // Validar que està en estat PENDING
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "La comanda " + orderUuid + " no es pot enviar. Estat actual: " + order.getStatus());
        }

        // Enviar notificació per email
        // Si falla, la transacció fa rollback i l'estat no canvia
        notificationService.sendOrderNotification(order);

        // Actualitzar estat (ja es fa dins de notificationService, però per seguretat)
        order.setStatus(Order.OrderStatus.SENT);
        orderRepository.save(order);

        log.info("Comanda {} enviada correctament a {}",
                orderUuid, order.getSupplier().getEmail());

        return buildOrderResponseDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO getOrderByUuid(String uuid){

        // Buscar la comanda
        Order order = orderRepository.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("No s'ha trobat cap comanda amb el UUID: " + uuid));

        // Retornar DTO
        return buildOrderResponseDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO deleteOrder(String orderUuid){
        // Buscar la comanda
        Order order = orderRepository.findByUuid(orderUuid).orElseThrow(() -> new ResourceNotFoundException("No s'ha trobat cap comanda amb el UUID: " + orderUuid));

        // Marcar com a inactiu
        order.setStatus(Order.OrderStatus.DELETED);

        // Guardar canvis
        order = orderRepository.save(order);

        // Retornar DTO
        return buildOrderResponseDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrder(String uuid, OrderRequestDTO dto) {

        // Buscar la comanda pel UUID
        Order order = orderRepository.findByUuid(uuid)
                .orElseThrow(() -> new BadRequestException("La comanda no existeix"));

        // Comprovar que no estigui esborrada
        if (order.getStatus() == Order.OrderStatus.DELETED) {
            throw new BadRequestException("No es pot modificar una comanda eliminada");
        }

        // Actualitzar camps simples
        order.setName(dto.getName());
        order.setNotes(dto.getNotes());
        order.setDeliveryDate(dto.getDeliveryDate());

        // Comprovar proveïdor
        if (dto.getSupplierUuid() != null) {
            Supplier supplier = supplierRepository.findByUuid(dto.getSupplierUuid())
                    .orElseThrow(() -> new BadRequestException("Proveïdor no trobat"));
            order.setSupplier(supplier);
        }

        // Assignar company i usuari
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));
        order.setUser(user);
        order.setCompany(user.getCompany());

        // Gestionar items
        Map<String, OrderItem> currentItemsMap = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getUuid, i -> i));

        List<OrderItem> updatedItems = new ArrayList<>();

        for (OrderItemRequestDTO itemDTO : dto.getItems()) {
            if (itemDTO.getOrderItemUuid() != null) {
                // ITEM EXISTENT → UPDATE
                OrderItem existing = currentItemsMap.get(itemDTO.getOrderItemUuid());
                if (existing == null) {
                    throw new BadRequestException("Item amb UUID " + itemDTO.getOrderItemUuid() + " no existeix en aquesta comanda");
                }

                Product product = productRepository.findByUuid(itemDTO.getProductUuid())
                        .orElseThrow(() -> new ResourceNotFoundException("Producte no trobat: " + itemDTO.getProductUuid()));

                existing.setProduct(product);
                existing.setQuantity(itemDTO.getQuantity());
                existing.setUnitPrice(product.getPrice());
                existing.setSubtotal(product.getPrice().multiply(itemDTO.getQuantity()));
                existing.setNotes(itemDTO.getNotes());

                updatedItems.add(existing);
                currentItemsMap.remove(itemDTO.getOrderItemUuid()); // Traiem del map perquè ja està tractat

            } else {
                // ITEM NOU → CREATE
                Product product = productRepository.findByUuid(itemDTO.getProductUuid())
                        .orElseThrow(() -> new ResourceNotFoundException("Producte no trobat: " + itemDTO.getProductUuid()));

                OrderItem newItem = new OrderItem();
                newItem.setUuid(UUID.randomUUID().toString());
                newItem.setOrder(order);
                newItem.setProduct(product);
                newItem.setQuantity(itemDTO.getQuantity());
                newItem.setUnitPrice(product.getPrice());
                newItem.setSubtotal(product.getPrice().multiply(itemDTO.getQuantity()));
                newItem.setNotes(itemDTO.getNotes());

                updatedItems.add(newItem);
            }
        }

        // Assignar items actualitzats a la llista gestionada per JPA i recalcular total
        List<OrderItem> items = order.getItems(); // llista gestionada per JPA
        items.clear();                             // elimina els orphans
        items.addAll(updatedItems);                // afegeix els items finals

        // Recalcular total
        BigDecimal totalAmount = updatedItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // Guardar comanda
        orderRepository.save(order);

        return buildOrderResponseDTO(order);
    }

    private OrderResponseDTO buildOrderResponseDTO(Order order) {
        return OrderResponseDTO.builder()
                .uuid(order.getUuid())
                .name(order.getName())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .deliveryDate(order.getDeliveryDate())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .supplierUuid(order.getSupplier().getUuid())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponseDTO.builder()
                                .uuid(item.getUuid())
                                .productUuid(item.getProduct() != null ? item.getProduct().getUuid() : null)
                                .productName(item.getProduct() != null ? item.getProduct().getName() : null)
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .subtotal(item.getSubtotal())
                                .notes(item.getNotes())
                                .build())
                        .toList())
                .build();
    }

    private OrderResponseDTO mapToResponseDTO(Order order) {
        return OrderResponseDTO.builder()
                .uuid(order.getUuid())
                .name(order.getName())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .deliveryDate(order.getDeliveryDate())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .supplierUuid(order.getSupplier().getUuid())
                .items(order.getItems().stream().map(this::mapItemToDTO).toList())
                .build();
    }

    private OrderItemResponseDTO mapItemToDTO(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .uuid(item.getUuid())
                .productUuid(item.getProduct().getUuid())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .notes(item.getNotes())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ConsumptionAnalysisDTO getConsumptionAnalysis(int days) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));
        Long companyId = user.getCompany().getId();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(days);

        List<Order> orders = orderRepository.getOrdersByCompanyIdAndPeriodWithOrderItems(companyId, startDate, now);

        Set<Order.OrderStatus> validStatuses = Set.of(
                Order.OrderStatus.PENDING,
                Order.OrderStatus.SENT,
                Order.OrderStatus.CONFIRMED,
                Order.OrderStatus.COMPLETED
        );
        List<Order> filteredOrders = orders.stream()
                .filter(o -> validStatuses.contains(o.getStatus()))
                .collect(Collectors.toList());

        int totalOrders = filteredOrders.size();
        BigDecimal totalSpent = filteredOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageOrderAmount = totalOrders > 0
                ? totalSpent.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        record TaggedItem(OrderItem item, Long orderId, LocalDateTime orderCreatedAt) {}

        List<TaggedItem> taggedItems = filteredOrders.stream()
                .flatMap(order -> order.getItems().stream()
                        .filter(item -> item.getProduct() != null)
                        .map(item -> new TaggedItem(item, order.getId(), order.getCreatedAt())))
                .collect(Collectors.toList());

        Map<Long, List<TaggedItem>> itemsByProduct = taggedItems.stream()
                .collect(Collectors.groupingBy(ti -> ti.item().getProduct().getId()));

        List<ProductConsumptionDTO> topProducts = itemsByProduct.values().stream()
                .map(productItems -> {
                    Product product = productItems.get(0).item().getProduct();

                    BigDecimal totalQuantity = productItems.stream()
                            .map(ti -> ti.item().getQuantity())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long orderCount = productItems.stream()
                            .map(TaggedItem::orderId)
                            .distinct()
                            .count();

                    BigDecimal productTotalSpent = productItems.stream()
                            .map(ti -> ti.item().getUnitPrice().multiply(ti.item().getQuantity()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal avgQuantityPerOrder = orderCount > 0
                            ? totalQuantity.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    String supplierName = product.getSupplier() != null ? product.getSupplier().getName() : null;

                    return ProductConsumptionDTO.builder()
                            .productUuid(product.getUuid())
                            .productName(product.getName())
                            .supplierName(supplierName)
                            .category(product.getCategory())
                            .totalQuantity(totalQuantity)
                            .unit(product.getUnit())
                            .totalSpent(productTotalSpent)
                            .orderCount((int) orderCount)
                            .avgQuantityPerOrder(avgQuantityPerOrder)
                            .currentPrice(product.getPrice())
                            .build();
                })
                .sorted(Comparator.comparing(ProductConsumptionDTO::getOrderCount).reversed()
                        .thenComparing(Comparator.comparing(ProductConsumptionDTO::getTotalSpent).reversed()))
                .limit(50)
                .collect(Collectors.toList());

        Map<Long, List<Order>> ordersBySupplier = filteredOrders.stream()
                .filter(o -> o.getSupplier() != null)
                .collect(Collectors.groupingBy(o -> o.getSupplier().getId()));

        List<SupplierSpendDTO> spendBySupplier = ordersBySupplier.values().stream()
                .map(supplierOrders -> {
                    Supplier supplier = supplierOrders.get(0).getSupplier();

                    BigDecimal supplierTotal = supplierOrders.stream()
                            .map(Order::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal percentage = totalSpent.compareTo(BigDecimal.ZERO) > 0
                            ? supplierTotal.divide(totalSpent, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return SupplierSpendDTO.builder()
                            .supplierUuid(supplier.getUuid())
                            .supplierName(supplier.getName())
                            .totalSpent(supplierTotal)
                            .percentage(percentage)
                            .orderCount(supplierOrders.size())
                            .build();
                })
                .sorted(Comparator.comparing(SupplierSpendDTO::getTotalSpent).reversed())
                .collect(Collectors.toList());

        List<PriceTrendDTO> priceTrends = itemsByProduct.values().stream()
                .map(productItems -> {
                    Product product = productItems.get(0).item().getProduct();

                    List<TaggedItem> sorted = productItems.stream()
                            .sorted(Comparator.comparing(TaggedItem::orderCreatedAt))
                            .collect(Collectors.toList());

                    BigDecimal oldestPrice = sorted.get(0).item().getUnitPrice();
                    BigDecimal latestPrice = sorted.get(sorted.size() - 1).item().getUnitPrice();

                    BigDecimal changePercent = BigDecimal.ZERO;
                    String trend = "STABLE";

                    if (oldestPrice.compareTo(BigDecimal.ZERO) > 0) {
                        changePercent = latestPrice.subtract(oldestPrice)
                                .divide(oldestPrice, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);

                        if (changePercent.compareTo(new BigDecimal("2")) > 0) {
                            trend = "UP";
                        } else if (changePercent.compareTo(new BigDecimal("-2")) < 0) {
                            trend = "DOWN";
                        }
                    }

                    String supplierName = product.getSupplier() != null ? product.getSupplier().getName() : null;

                    return PriceTrendDTO.builder()
                            .productUuid(product.getUuid())
                            .productName(product.getName())
                            .supplierName(supplierName)
                            .oldestPrice(oldestPrice)
                            .latestPrice(latestPrice)
                            .changePercent(changePercent)
                            .trend(trend)
                            .build();
                })
                .collect(Collectors.toList());

        return ConsumptionAnalysisDTO.builder()
                .totalOrders(totalOrders)
                .totalSpent(totalSpent)
                .averageOrderAmount(averageOrderAmount)
                .analyzedDays(days)
                .periodStart(startDate)
                .periodEnd(now)
                .topProducts(topProducts)
                .spendBySupplier(spendBySupplier)
                .priceTrends(priceTrends)
                .build();
    }



}