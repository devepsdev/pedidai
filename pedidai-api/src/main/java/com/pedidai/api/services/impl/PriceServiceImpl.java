package com.pedidai.api.services.impl;

import com.pedidai.api.dto.PriceAlertDTO;
import com.pedidai.api.dto.PriceGroupDTO;
import com.pedidai.api.dto.PriceOfferDTO;
import com.pedidai.api.dto.PriceOverviewDTO;
import com.pedidai.api.entities.PriceHistory;
import com.pedidai.api.entities.Product;
import com.pedidai.api.repositories.PriceHistoryRepository;
import com.pedidai.api.repositories.ProductRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.services.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {

    /** Variació mínima (en %) perquè una pujada es mostri com a alerta. */
    private static final BigDecimal ALERT_THRESHOLD = new BigDecimal("2");

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    private final CurrentUser currentUser;

    @Override
    @Transactional
    public boolean recordObservation(Product product, BigDecimal unitPrice, String unit, BigDecimal quantity,
                                     LocalDate documentDate, PriceHistory.Source source, String documentRef) {
        if (unitPrice == null || unitPrice.signum() < 0) {
            return false;
        }
        LocalDate date = documentDate != null ? documentDate : LocalDate.now();
        Optional<PriceHistory> latest = priceHistoryRepository.findFirstByProduct_IdOrderByDocumentDateDescIdDesc(product.getId());

        priceHistoryRepository.save(PriceHistory.builder()
                .company(product.getSupplier().getCompany())
                .supplier(product.getSupplier())
                .product(product)
                .unitPrice(unitPrice)
                .unit(unit != null ? unit : product.getUnit())
                .quantity(quantity)
                .documentDate(date)
                .source(source)
                .documentRef(documentRef)
                .build());

        boolean isMostRecent = latest.isEmpty() || !date.isBefore(latest.get().getDocumentDate());
        BigDecimal rounded = unitPrice.setScale(2, RoundingMode.HALF_UP);
        if (isMostRecent && (product.getPrice() == null || product.getPrice().compareTo(rounded) != 0)) {
            product.setPrice(rounded);
            productRepository.save(product);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public PriceOverviewDTO getOverview(int days) {
        int window = Math.max(1, Math.min(days, 730));
        Long companyId = currentUser.companyId();
        // Es miren fins a 2 anys enrere per conèixer el preu vigent, encara que la finestra sigui més curta
        List<PriceHistory> history = priceHistoryRepository.findActiveByCompanySince(companyId, LocalDate.now().minusYears(2));

        List<PriceGroupDTO> groups = buildGroups(history);
        Map<String, BigDecimal> cheapestByGroup = groups.stream()
                .filter(g -> g.getOffers().size() > 1)
                .collect(Collectors.toMap(PriceGroupDTO::getKey, PriceGroupDTO::getCheapestPrice));

        // Sobrecost: línies d'albarà del període pagades per sobre del preu més barat vigent
        LocalDate from = LocalDate.now().minusDays(window);
        BigDecimal overpaid = BigDecimal.ZERO;
        int overpaidLines = 0;
        for (PriceHistory h : history) {
            if (h.getSource() != PriceHistory.Source.INVOICE || h.getQuantity() == null
                    || h.getDocumentDate().isBefore(from)) {
                continue;
            }
            BigDecimal cheapest = cheapestByGroup.get(groupKey(h.getProduct(), h.getUnit()));
            if (cheapest != null && h.getUnitPrice().compareTo(cheapest) > 0) {
                overpaid = overpaid.add(h.getUnitPrice().subtract(cheapest).multiply(h.getQuantity()));
                overpaidLines++;
            }
        }

        List<PriceAlertDTO> alerts = buildAlerts(history, from);
        List<PriceGroupDTO> comparable = groups.stream().filter(g -> g.getOffers().size() > 1)
                .sorted(Comparator.comparing(PriceGroupDTO::getSpreadPercent, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<PriceGroupDTO> single = groups.stream().filter(g -> g.getOffers().size() == 1)
                .sorted(Comparator.comparing(PriceGroupDTO::getName))
                .toList();

        return PriceOverviewDTO.builder()
                .days(window)
                .observations((int) history.stream().filter(h -> !h.getDocumentDate().isBefore(from)).count())
                .productsTracked((int) history.stream().map(h -> h.getProduct().getId()).distinct().count())
                .suppliersTracked((int) history.stream().map(h -> h.getSupplier().getId()).distinct().count())
                .comparable(comparable)
                .singleSupplier(single)
                .alerts(alerts)
                .overpaidAmount(overpaid.setScale(2, RoundingMode.HALF_UP))
                .overpaidLines(overpaidLines)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceGroupDTO> compareByName(String productName, int days) {
        String needle = ProductNames.canonical(productName);
        if (needle == null) {
            return List.of();
        }
        List<PriceHistory> history = priceHistoryRepository.findActiveByCompanySince(
                currentUser.companyId(), LocalDate.now().minusYears(2));
        // Coincidència per paraules: "tomates" troba "tomate pera" i "tomàquet" no (cal el nom genèric)
        List<String> words = Arrays.stream(needle.split(" ")).map(ProductNames::singular).toList();
        return buildGroups(history).stream()
                .filter(g -> {
                    String haystack = ProductNames.canonical(g.getName() + " "
                            + g.getOffers().stream().map(PriceOfferDTO::getProductName).collect(Collectors.joining(" ")));
                    List<String> tokens = Arrays.stream(haystack.split(" ")).map(ProductNames::singular).toList();
                    return tokens.containsAll(words);
                })
                .sorted(Comparator.comparing((PriceGroupDTO g) -> g.getOffers().size()).reversed())
                .toList();
    }

    // ───────────────────────── Càlculs ─────────────────────────

    private List<PriceGroupDTO> buildGroups(List<PriceHistory> history) {
        // Darrera i penúltima observació de cada producte
        Map<Long, List<PriceHistory>> byProduct = history.stream()
                .collect(Collectors.groupingBy(h -> h.getProduct().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<String, List<PriceOfferDTO>> offersByGroup = new LinkedHashMap<>();
        Map<String, String[]> groupLabels = new HashMap<>();

        for (List<PriceHistory> observations : byProduct.values()) {
            observations.sort(Comparator.comparing(PriceHistory::getDocumentDate).thenComparing(PriceHistory::getId));
            PriceHistory latest = observations.get(observations.size() - 1);
            PriceHistory previous = findPreviousDifferentDate(observations);
            Product product = latest.getProduct();

            BigDecimal change = previous != null ? percentChange(previous.getUnitPrice(), latest.getUnitPrice()) : null;
            String unit = ProductNames.unit(latest.getUnit() != null ? latest.getUnit() : product.getUnit());
            String key = groupKey(product, latest.getUnit());

            offersByGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(PriceOfferDTO.builder()
                    .productUuid(product.getUuid())
                    .productName(product.getName())
                    .supplierUuid(latest.getSupplier().getUuid())
                    .supplierName(latest.getSupplier().getName())
                    .supplierHasEmail(latest.getSupplier().getEmail() != null && !latest.getSupplier().getEmail().isBlank())
                    .unit(unit)
                    .latestPrice(scale(latest.getUnitPrice()))
                    .latestDate(latest.getDocumentDate())
                    .previousPrice(previous != null ? scale(previous.getUnitPrice()) : null)
                    .changePercent(change)
                    .build());
            groupLabels.putIfAbsent(key, new String[]{displayName(product), unit});
        }

        List<PriceGroupDTO> groups = new ArrayList<>();
        offersByGroup.forEach((key, offers) -> {
            offers.sort(Comparator.comparing(PriceOfferDTO::getLatestPrice));
            BigDecimal cheapest = offers.get(0).getLatestPrice();
            BigDecimal highest = offers.get(offers.size() - 1).getLatestPrice();
            if (offers.size() > 1) {
                offers.stream().filter(o -> o.getLatestPrice().compareTo(cheapest) == 0).forEach(o -> o.setCheapest(true));
            }
            groups.add(PriceGroupDTO.builder()
                    .key(key)
                    .name(groupLabels.get(key)[0])
                    .unit(groupLabels.get(key)[1])
                    .offers(offers)
                    .cheapestPrice(cheapest)
                    .highestPrice(highest)
                    .spreadPercent(offers.size() > 1 ? percentChange(cheapest, highest) : null)
                    .build());
        });
        return groups;
    }

    /** Pujades de preu (≥ 2 %) entre dues observacions consecutives del mateix producte dins el període. */
    private List<PriceAlertDTO> buildAlerts(List<PriceHistory> history, LocalDate from) {
        Map<Long, List<PriceHistory>> byProduct = history.stream()
                .collect(Collectors.groupingBy(h -> h.getProduct().getId()));
        List<PriceAlertDTO> alerts = new ArrayList<>();
        for (List<PriceHistory> observations : byProduct.values()) {
            observations.sort(Comparator.comparing(PriceHistory::getDocumentDate).thenComparing(PriceHistory::getId));
            PriceHistory latest = observations.get(observations.size() - 1);
            PriceHistory previous = findPreviousDifferentDate(observations);
            if (previous == null || latest.getDocumentDate().isBefore(from)) {
                continue;
            }
            BigDecimal change = percentChange(previous.getUnitPrice(), latest.getUnitPrice());
            if (change != null && change.compareTo(ALERT_THRESHOLD) >= 0) {
                alerts.add(PriceAlertDTO.builder()
                        .productUuid(latest.getProduct().getUuid())
                        .productName(latest.getProduct().getName())
                        .supplierName(latest.getSupplier().getName())
                        .unit(ProductNames.unit(latest.getUnit()))
                        .previousPrice(scale(previous.getUnitPrice()))
                        .previousDate(previous.getDocumentDate())
                        .latestPrice(scale(latest.getUnitPrice()))
                        .latestDate(latest.getDocumentDate())
                        .changePercent(change)
                        .build());
            }
        }
        alerts.sort(Comparator.comparing(PriceAlertDTO::getChangePercent).reversed());
        return alerts;
    }

    /** Observació anterior a la darrera amb data de document diferent (evita comparar línies del mateix albarà). */
    private static PriceHistory findPreviousDifferentDate(List<PriceHistory> sorted) {
        PriceHistory latest = sorted.get(sorted.size() - 1);
        for (int i = sorted.size() - 2; i >= 0; i--) {
            if (sorted.get(i).getDocumentDate().isBefore(latest.getDocumentDate())) {
                return sorted.get(i);
            }
        }
        return null;
    }

    static String groupKey(Product product, String unit) {
        String name = ProductNames.canonical(product.getCanonicalName() != null ? product.getCanonicalName() : product.getName());
        return name + "|" + ProductNames.unit(unit != null ? unit : product.getUnit());
    }

    private static String displayName(Product product) {
        String c = product.getCanonicalName();
        if (c == null || c.isBlank()) {
            return product.getName();
        }
        return Character.toUpperCase(c.charAt(0)) + c.substring(1);
    }

    private static BigDecimal percentChange(BigDecimal from, BigDecimal to) {
        if (from == null || to == null || from.signum() == 0) {
            return null;
        }
        return to.subtract(from).multiply(BigDecimal.valueOf(100)).divide(from, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
