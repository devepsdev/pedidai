package com.pedidai.api.services.impl;

import com.pedidai.api.repositories.*;
import com.pedidai.api.services.DataRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Compleix el termini de conservació de la política de privacitat: si una empresa no contracta,
 * les seves dades es conserven 30 dies després del final de la prova i després s'esborren.
 * Les empreses de pagament ({@code trialEndsAt} nul) i la del SUPER_ADMIN no s'esborren mai.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionServiceImpl implements DataRetentionService {

    public static final int RETENTION_DAYS = 30;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Scheduled(cron = "${app.retention.cron:0 30 3 * * *}", zone = "Europe/Madrid")
    public int purgeExpiredTrials() {
        LocalDateTime limit = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = 0;
        for (Long companyId : companyRepository.findIdsWithTrialExpiredBefore(limit)) {
            try {
                // Cada empresa en la seva pròpia transacció: si una falla, les altres continuen
                List<String> images = transactionTemplate.execute(status -> deleteCompany(companyId));
                deleteUnusedImages(images);
                deleted++;
                log.info("Dades esborrades per fi de conservació: empresa {}", companyId);
            } catch (RuntimeException e) {
                log.error("No s'han pogut esborrar les dades de l'empresa {}", companyId, e);
            }
        }
        return deleted;
    }

    /** Esborra l'empresa i tot el que en depèn, en l'ordre que demanen les claus foranes. */
    private List<String> deleteCompany(Long companyId) {
        List<String> images = productRepository.findImageUrlsByCompanyId(companyId);
        priceHistoryRepository.deleteByCompanyId(companyId);
        orderItemRepository.deleteByCompanyId(companyId);
        orderRepository.deleteByCompanyId(companyId);
        productRepository.deleteByCompanyId(companyId);
        supplierRepository.deleteByCompanyId(companyId);
        userRepository.deleteByCompanyId(companyId);
        companyRepository.deleteCompanyById(companyId);
        return images;
    }

    /** Esborra del disc les imatges pujades que ja no fa servir cap producte. */
    private void deleteUnusedImages(List<String> urls) {
        if (urls == null) {
            return;
        }
        Path dir = Paths.get(ProductServiceImpl.IMAGE_DIR).toAbsolutePath().normalize();
        for (String url : urls) {
            if (!url.startsWith(ProductServiceImpl.IMAGE_URL_PREFIX) || productRepository.existsByImageUrl(url)) {
                continue;
            }
            Path file = dir.resolve(url.substring(ProductServiceImpl.IMAGE_URL_PREFIX.length())).normalize();
            if (!file.startsWith(dir)) {
                continue;
            }
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("No s'ha pogut esborrar la imatge {}", file, e);
            }
        }
    }
}
