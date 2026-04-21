package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.*;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.*;
import com.pedidai.api.services.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperAdminServiceImpl implements SuperAdminService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    private static final List<Order.OrderStatus> EXCLUDED_STATUSES =
            List.of(Order.OrderStatus.DELETED, Order.OrderStatus.CANCELLED);

    @Override
    public SuperAdminDashboardDTO getDashboard() {
        long totalCompanies = companyRepository.count();
        long activeCompanies = companyRepository.countByStatus(Company.CompanyStatus.ACTIVE);
        long inactiveCompanies = companyRepository.countByStatus(Company.CompanyStatus.INACTIVE);
        long suspendedCompanies = companyRepository.countByStatus(Company.CompanyStatus.SUSPENDED);
        long pendingCompanies = companyRepository.countByStatus(Company.CompanyStatus.PENDING);

        long totalUsers = userRepository.countByIsDeletedFalse();
        long totalAdmins = userRepository.countByRole(User.UserRole.ADMIN);
        long totalRegularUsers = userRepository.countByRole(User.UserRole.USER);

        LocalDateTime since30d = LocalDateTime.now().minusDays(30);
        long totalOrders30d = orderRepository.countRecentOrders(since30d, EXCLUDED_STATUSES);
        BigDecimal totalSpent30d = orderRepository.sumRecentOrdersAmount(since30d, EXCLUDED_STATUSES);
        if (totalSpent30d == null) totalSpent30d = BigDecimal.ZERO;

        BigDecimal potentialMRR = BigDecimal.valueOf(activeCompanies).multiply(BigDecimal.valueOf(29));

        // Top 10 companies by order count in last 30d
        Pageable top10 = PageRequest.of(0, 10);
        List<Object[]> rawTop = orderRepository.findTopCompaniesByOrderCount(since30d, EXCLUDED_STATUSES, top10);
        List<TopCompanyDTO> topCompanies = rawTop.stream()
                .map(row -> TopCompanyDTO.builder()
                        .companyUuid((String) row[0])
                        .companyName((String) row[1])
                        .orderCount(((Number) row[2]).longValue())
                        .totalSpent(row[3] instanceof BigDecimal ? (BigDecimal) row[3] : BigDecimal.valueOf(((Number) row[3]).doubleValue()))
                        .build())
                .collect(Collectors.toList());

        return SuperAdminDashboardDTO.builder()
                .totalCompanies(totalCompanies)
                .activeCompanies(activeCompanies)
                .inactiveCompanies(inactiveCompanies)
                .suspendedCompanies(suspendedCompanies)
                .pendingCompanies(pendingCompanies)
                .totalUsers(totalUsers)
                .totalAdmins(totalAdmins)
                .totalRegularUsers(totalRegularUsers)
                .totalOrders30d(totalOrders30d)
                .totalSpent30d(totalSpent30d)
                .potentialMRR(potentialMRR)
                .topCompanies(topCompanies)
                .build();
    }

    @Override
    public Page<CompanySummaryDTO> getCompanies(String search, String statusStr, Pageable pageable) {
        Company.CompanyStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try { status = Company.CompanyStatus.valueOf(statusStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        Page<Company> page = companyRepository.findByFilters(status, searchParam, pageable);
        return page.map(this::toSummaryDTO);
    }

    @Override
    public CompanyDetailDTO getCompanyDetail(String uuid) {
        Company company = companyRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no trobada: " + uuid));

        List<User> users = userRepository.findByCompanyIdNotDeleted(company.getId());
        List<UserSummaryDTO> userDTOs = users.stream().map(this::toUserSummaryDTO).collect(Collectors.toList());

        long orderCount = companyRepository.countOrdersByCompanyId(company.getId());

        // Recent orders: get last 10 orders for the company
        LocalDateTime from = LocalDateTime.now().minusYears(10);
        LocalDateTime to = LocalDateTime.now();
        List<Order> recentOrders = orderRepository.getOrdersByCompanyIdAndPeriodWithoutOrderItems(company.getId(), from, to)
                .stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<CompanyDetailDTO.RecentOrderDTO> recentDTOs = recentOrders.stream()
                .map(o -> CompanyDetailDTO.RecentOrderDTO.builder()
                        .uuid(o.getUuid())
                        .supplierName(o.getSupplier() != null ? o.getSupplier().getName() : "")
                        .total(o.getTotalAmount())
                        .status(o.getStatus().name())
                        .createdAt(o.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return CompanyDetailDTO.builder()
                .uuid(company.getUuid())
                .name(company.getName())
                .taxId(company.getTaxId())
                .email(company.getEmail())
                .phone(company.getPhone())
                .address(company.getAddress())
                .city(company.getCity())
                .postalCode(company.getPostalCode())
                .status(company.getStatus().name())
                .createdAt(company.getCreatedAt())
                .trialEndsAt(company.getTrialEndsAt())
                .users(userDTOs)
                .supplierCount(0) // simplified
                .productCount(0)  // simplified
                .orderCount(orderCount)
                .recentOrders(recentDTOs)
                .build();
    }

    @Override
    @Transactional
    public CompanySummaryDTO updateCompanyStatus(String uuid, String statusStr) {
        Company company = companyRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no trobada: " + uuid));
        try {
            company.setStatus(Company.CompanyStatus.valueOf(statusStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new com.pedidai.api.exceptions.BadRequestException("Status invàlid: " + statusStr);
        }
        companyRepository.save(company);
        return toSummaryDTO(company);
    }

    @Override
    public Page<UserAdminDTO> getUsers(String search, String roleStr, String companyUuid, Pageable pageable) {
        User.UserRole role = null;
        if (roleStr != null && !roleStr.isBlank()) {
            try { role = User.UserRole.valueOf(roleStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        String companyParam = (companyUuid != null && !companyUuid.isBlank()) ? companyUuid : null;
        Page<User> page = userRepository.findByAdminFilters(role, companyParam, searchParam, pageable);
        return page.map(this::toUserAdminDTO);
    }

    @Override
    public List<MonthlyStatsDTO> getMonthlyStats() {
        LocalDateTime since = LocalDateTime.now().minusMonths(12);
        List<Object[]> raw = orderRepository.findMonthlyOrderStats(since);
        return raw.stream()
                .map(row -> MonthlyStatsDTO.builder()
                        .month((String) row[0])
                        .orderCount(((Number) row[1]).longValue())
                        .totalSpent(row[2] instanceof BigDecimal ? (BigDecimal) row[2] : BigDecimal.valueOf(((Number) row[2]).doubleValue()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CompanySummaryDTO extendTrial(String uuid, int months) {
        Company company = companyRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no trobada: " + uuid));
        // Si ya tenía trial, extender desde hoy; si no tenía, iniciar desde hoy
        company.setTrialEndsAt(LocalDateTime.now().plusMonths(months));
        // Si estaba INACTIVE por trial expirado, la reactivamos
        if (company.getStatus() == Company.CompanyStatus.INACTIVE) {
            company.setStatus(Company.CompanyStatus.ACTIVE);
        }
        companyRepository.save(company);
        return toSummaryDTO(company);
    }

    @Override
    @Transactional
    public CompanySummaryDTO activateCompany(String uuid, String plan) {
        Company company = companyRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no trobada: " + uuid));
        // Cliente de pago: sin límite de trial, estado ACTIVE
        company.setTrialEndsAt(null);
        company.setStatus(Company.CompanyStatus.ACTIVE);
        companyRepository.save(company);
        return toSummaryDTO(company);
    }

    // ---- Mappers ----

    private CompanySummaryDTO toSummaryDTO(Company c) {
        long userCount = companyRepository.countUsersByCompanyId(c.getId());
        long orderCount = companyRepository.countOrdersByCompanyId(c.getId());
        Optional<LocalDateTime> lastOrder = companyRepository.findLastOrderDateByCompanyId(c.getId());
        return CompanySummaryDTO.builder()
                .uuid(c.getUuid())
                .name(c.getName())
                .taxId(c.getTaxId())
                .email(c.getEmail())
                .city(c.getCity())
                .status(c.getStatus().name())
                .userCount(userCount)
                .orderCount(orderCount)
                .createdAt(c.getCreatedAt())
                .trialEndsAt(c.getTrialEndsAt())
                .lastOrderDate(lastOrder.orElse(null))
                .build();
    }

    private UserSummaryDTO toUserSummaryDTO(User u) {
        return UserSummaryDTO.builder()
                .uuid(u.getUuid())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .role(u.getRole().name())
                .active(Boolean.TRUE.equals(u.getIsActive()))
                .lastLogin(u.getLastLogin())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private UserAdminDTO toUserAdminDTO(User u) {
        return UserAdminDTO.builder()
                .uuid(u.getUuid())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .role(u.getRole().name())
                .active(Boolean.TRUE.equals(u.getIsActive()))
                .companyName(u.getCompany() != null ? u.getCompany().getName() : "")
                .companyUuid(u.getCompany() != null ? u.getCompany().getUuid() : "")
                .createdAt(u.getCreatedAt())
                .build();
    }
}
