package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.*;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ForbiddenException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.exceptions.TooManyRequestsException;
import com.pedidai.api.repositories.*;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.services.NotificationService;
import com.pedidai.api.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    /** Màxim de comandes enviades per empresa i dia (evita l'ús de PedidAI per enviar correu massiu). */
    static final int MAX_SENT_PER_DAY = 30;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO dto) {
        User user = currentUser.get();
        Company company = CurrentUser.companyOf(user);
        Supplier supplier = findOwnedSupplier(dto.getSupplierUuid(), company.getId());

        Order order = new Order();
        order.setUuid(UUID.randomUUID().toString());
        order.setName(dto.getName().trim());
        order.setNotes(dto.getNotes());
        order.setDeliveryDate(dto.getDeliveryDate());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setUser(user);
        order.setCompany(company);
        order.setSupplier(supplier);

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequestDTO itemDTO : dto.getItems()) {
            OrderItem item = new OrderItem();
            item.setUuid(UUID.randomUUID().toString());
            item.setOrder(order);
            fillItem(item, itemDTO, supplier);
            items.add(item);
        }
        order.setItems(items);
        order.setTotalAmount(total(items));

        orderRepository.save(order);
        orderItemRepository.saveAll(items);
        log.info("Comanda {} creada per l'usuari {} (empresa {})", order.getUuid(), user.getUuid(), company.getId());
        return mapToResponseDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> filterOrders(OrderFilterDTO dto, Pageable pageable) {
        Long companyId = currentUser.companyId();
        Long orderId = null;
        Long supplierId = null;
        Long userId = null;

        if (dto.getOrderUuid() != null && !dto.getOrderUuid().isBlank()) {
            orderId = findOwned(dto.getOrderUuid(), companyId).getId();
        }
        if (dto.getSupplierUuid() != null && !dto.getSupplierUuid().isBlank()) {
            supplierId = findOwnedSupplier(dto.getSupplierUuid(), companyId).getId();
        }
        if (dto.getUserUuid() != null && !dto.getUserUuid().isBlank()) {
            userId = userRepository.findByUuidAndCompany_Id(dto.getUserUuid(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound")).getId();
        }

        Order.OrderStatus status = null;
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            try {
                status = Order.OrderStatus.valueOf(dto.getStatus().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("error.order.invalidStatus");
            }
        }

        var spec = OrderSpecifications.filterOrders(
                orderId, companyId, supplierId, userId,
                dto.getSearchText(), dto.getName(), dto.getNotes(), status,
                dto.getMinAmount(), dto.getMaxAmount(),
                dto.getDeliveryDateFrom(), dto.getDeliveryDateTo(),
                dto.getCreatedAtFrom(), dto.getCreatedAtTo(),
                dto.getUpdatedAtFrom(), dto.getUpdatedAtTo()
        );
        return orderRepository.findAll(spec, pageable).map(this::mapToResponseDTO);
    }

    @Override
    @Transactional
    public OrderResponseDTO sendOrder(String orderUuid) {
        User user = currentUser.get();
        Order order = findOwned(orderUuid, CurrentUser.companyOf(user).getId());

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BadRequestException("error.order.notPending");
        }
        // Els correus als proveïdors surten de PedidAI: cal haver verificat l'email del compte
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ForbiddenException("error.order.emailNotVerified");
        }
        long sentToday = orderRepository.countByCompany_IdAndStatusAndUpdatedAtAfter(
                order.getCompany().getId(), Order.OrderStatus.SENT, LocalDateTime.now().minusDays(1));
        if (sentToday >= MAX_SENT_PER_DAY) {
            throw new TooManyRequestsException("error.order.dailyLimit");
        }

        // Si l'enviament falla, la transacció es desfà i la comanda continua pendent
        notificationService.sendOrderNotification(order);
        order.setStatus(Order.OrderStatus.SENT);
        orderRepository.save(order);
        return mapToResponseDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByUuid(String uuid) {
        return mapToResponseDTO(findOwned(uuid, currentUser.companyId()));
    }

    @Override
    @Transactional
    public OrderResponseDTO deleteOrder(String orderUuid) {
        Order order = findOwned(orderUuid, currentUser.companyId());
        order.setStatus(Order.OrderStatus.DELETED);
        return mapToResponseDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrder(String uuid, OrderRequestDTO dto) {
        User user = currentUser.get();
        Long companyId = CurrentUser.companyOf(user).getId();
        Order order = findOwned(uuid, companyId);

        // Una comanda ja enviada no es pot canviar sense que el proveïdor se n'assabenti
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BadRequestException("error.order.notEditable");
        }

        order.setName(dto.getName().trim());
        order.setNotes(dto.getNotes());
        order.setDeliveryDate(dto.getDeliveryDate());
        if (dto.getSupplierUuid() != null) {
            order.setSupplier(findOwnedSupplier(dto.getSupplierUuid(), companyId));
        }
        order.setUser(user);

        Map<String, OrderItem> currentItems = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getUuid, i -> i));
        List<OrderItem> updatedItems = new ArrayList<>();
        for (OrderItemRequestDTO itemDTO : dto.getItems()) {
            OrderItem item;
            if (itemDTO.getOrderItemUuid() != null) {
                item = currentItems.remove(itemDTO.getOrderItemUuid());
                if (item == null) {
                    throw new BadRequestException("error.order.itemNotInOrder");
                }
            } else {
                item = new OrderItem();
                item.setUuid(UUID.randomUUID().toString());
                item.setOrder(order);
            }
            fillItem(item, itemDTO, order.getSupplier());
            updatedItems.add(item);
        }

        List<OrderItem> items = order.getItems();
        items.clear();
        items.addAll(updatedItems);
        order.setTotalAmount(total(updatedItems));
        orderRepository.save(order);
        return mapToResponseDTO(order);
    }

    // ───────────────────────── Utilitats ─────────────────────────

    /** Omple una línia amb el producte (que ha de ser del proveïdor de la comanda) i el seu preu vigent. */
    private void fillItem(OrderItem item, OrderItemRequestDTO dto, Supplier supplier) {
        Product product = productRepository.findByUuidAndSupplier_Company_Id(dto.getProductUuid(), supplier.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("error.product.notFound"));
        if (!product.getSupplier().getId().equals(supplier.getId())) {
            throw new BadRequestException("error.order.productFromOtherSupplier", product.getName(), supplier.getName());
        }
        item.setProduct(product);
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(product.getPrice());
        item.setSubtotal(dto.getQuantity().multiply(product.getPrice()).setScale(2, RoundingMode.HALF_UP));
        item.setNotes(dto.getNotes());
    }

    private static BigDecimal total(List<OrderItem> items) {
        return items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Order findOwned(String uuid, Long companyId) {
        return orderRepository.findByUuidAndCompany_Id(uuid, companyId)
                .filter(o -> o.getStatus() != Order.OrderStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("error.order.notFound"));
    }

    private Supplier findOwnedSupplier(String uuid, Long companyId) {
        return supplierRepository.findByUuidAndCompany_Id(uuid, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("error.supplier.notFound"));
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
                .supplierName(order.getSupplier().getName())
                .items(order.getItems().stream().map(this::mapItemToDTO).toList())
                .build();
    }

    private OrderItemResponseDTO mapItemToDTO(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .uuid(item.getUuid())
                .productUuid(item.getProduct() != null ? item.getProduct().getUuid() : null)
                .productName(item.getProduct() != null ? item.getProduct().getName() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .notes(item.getNotes())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ConsumptionAnalysisDTO getConsumptionAnalysis(int days) {
        Long companyId = currentUser.companyId();

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