package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.*;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.OrderRepository;
import com.pedidai.api.repositories.ProductRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.config.Messages;
import com.pedidai.api.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import com.pedidai.api.services.ReportService;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final CurrentUser currentUser;
    private final Messages messages;

    public ReportServiceImpl(OrderRepository orderRepository, CurrentUser currentUser, Messages messages) {
        this.orderRepository = orderRepository;
        this.currentUser = currentUser;
        this.messages = messages;
    }

    @Override
    @Transactional
    public DashboardResponseDTO dashboardInfo() {

        // Recuperem informació de l'usuari i la companyia
        Long companyId = currentUser.companyId();

        // Calculem el període (últim mes)
        LocalDateTime currentDate = LocalDateTime.now();
        YearMonth currentYearMonth = YearMonth.from(currentDate);
        LocalDateTime startDate = currentYearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = currentYearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        // Recuperar llistat de comandes
        List<Order> orders = orderRepository.getOrdersByCompanyIdAndPeriodWithoutOrderItems(companyId, startDate, endDate);

        // Recuperem només les actives
        List<Order> activeOrders = orders.stream().filter(o -> EnumSet.of(Order.OrderStatus.PENDING,Order.OrderStatus.SENT,Order.OrderStatus.CONFIRMED,Order.OrderStatus.COMPLETED).contains(o.getStatus())).toList();

        // Calculem el total de comandes
        int totalOrders = activeOrders.size();

        // Calculem l'import total
        BigDecimal totalAmount = activeOrders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculem els pendents
        int pendingOrders = (int) activeOrders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count();

        // Retornar el resultat com a DTO
        return mapToDashboardResponseDTO(totalOrders,totalAmount,pendingOrders);
    }

    @Override
    @Transactional
    public ReportGlobalResponseDTO globalInfo(PeriodRequestDTO dto) {

        // Recuperem informació de l'usuari i la companyia
        Long companyId = currentUser.companyId();

        // Recuperar llistat de comandes
        List<Order> orders = orderRepository.getOrdersByCompanyIdAndPeriodWithOrderItems(companyId, dto.getDataInicial(), dto.getDataFinal());

        // Recuperem només les actives
        List<Order> activeOrders = orders.stream().filter(o -> EnumSet.of(Order.OrderStatus.PENDING,Order.OrderStatus.SENT,Order.OrderStatus.CONFIRMED,Order.OrderStatus.COMPLETED).contains(o.getStatus())).toList();

        // Calculem el total de comandes
        int totalComandes = activeOrders.size();

        // Despesa total
        BigDecimal despesaTotal = activeOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(item -> item.getUnitPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);;

        // Comanda mitjana
        BigDecimal comandaMitjana = totalComandes > 0
                ? despesaTotal.divide(BigDecimal.valueOf(totalComandes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Despesa per proveïdor
        Map<Supplier, List<Order>> ordersBySupplier = activeOrders.stream().collect(Collectors.groupingBy(Order::getSupplier));

        List<DespesaPerProveidorDTO> despesaPerProveidor = ordersBySupplier.entrySet().stream()
                .map(entry -> {
                    Supplier supplier = entry.getKey();
                    List<Order> supplierOrders = entry.getValue();

                    // Nombre de comandes del proveïdor
                    int numComandes = supplierOrders.size();

                    // Despesa total del proveïdor
                    BigDecimal totalSupplier = supplierOrders.stream()
                            .flatMap(o -> o.getItems().stream())
                            .map(i -> i.getUnitPrice().multiply(i.getQuantity()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    // % respecte el total del període
                    BigDecimal percentatge = despesaTotal.compareTo(BigDecimal.ZERO) > 0
                            ? totalSupplier.multiply(BigDecimal.valueOf(100))
                            .divide(despesaTotal, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return new DespesaPerProveidorDTO(
                            supplier.getName(),
                            numComandes,
                            totalSupplier,
                            percentatge
                    );
                })
                .toList();

        // Agrupem tots els items per producte
        Map<Product, List<OrderItem>> itemsPerProduct = activeOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(OrderItem::getProduct));

        // Convertim a llista de DTO + càlculs
        List<ProducteTopDTO> topProductes = itemsPerProduct.entrySet().stream()
                .map(entry -> {
                    Product product = entry.getKey();
                    List<OrderItem> items = entry.getValue();

                    // Quantitat total
                    BigDecimal quantitatTotal = items.stream()
                            .map(OrderItem::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Despesa total
                    BigDecimal despesaProducte = items.stream()
                            .map(i -> i.getUnitPrice().multiply(i.getQuantity()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    return new ProducteTopDTO(
                            product.getName(),
                            quantitatTotal,
                            despesaProducte
                    );
                })
                // Ordenem pels més demanats
                .sorted(Comparator.comparing(ProducteTopDTO::getQuantitatTotal).reversed())
                // Top 10
                .limit(10)
                .toList();

        //Retornar el resultat com a DTO
        return mapToReportResponseDTO(
                dto.getDataInicial(),
                dto.getDataFinal(),
                totalComandes,
                despesaTotal,
                comandaMitjana,
                despesaPerProveidor,
                topProductes
        );
    }

    @Override
    @Transactional
    public byte[] generateGlobalInfoPDF(ReportGlobalResponseDTO report) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Logo PedidAI
            Image img = Image.getInstance(getClass().getResource("/images/logo_pdf.png"));
            img.scaleToFit(100,100);
            img.setAlignment(Image.ALIGN_RIGHT);


            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // Logo al document
            document.add(img);

            // Fonts
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLUE);
            Font subtitleFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK);

            // Títol
            Paragraph title = new Paragraph(messages.get("pdf.title"), titleFont);
            document.add(title);

            // Periode
            document.add(new Paragraph("\n"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            document.add(new Paragraph(messages.get("pdf.period", report.getDataInicial().format(formatter), report.getDataFinal().format(formatter))));

            // Informació general
            document.add(new Paragraph("\n"));
            Paragraph subtitleGlobal = new Paragraph(messages.get("pdf.summary"), subtitleFont);
            document.add(subtitleGlobal);
            document.add(new Paragraph("\n"));
            document.add(new Paragraph(messages.get("pdf.totalOrders") + ": " + report.getTotalComandes()));
            DecimalFormat df = new DecimalFormat("#0.00");
            String despesaTotalFormatada = df.format(report.getDespesaTotal());
            document.add(new Paragraph(messages.get("pdf.totalSpend") + ": " + despesaTotalFormatada + " €"));
            document.add(new Paragraph(messages.get("pdf.averageOrder") + ": " + df.format(report.getComandaMitjana()) + " €"));
            document.add(new Paragraph("\n"));

            // Tabla de proveïdors
            document.add(new Paragraph("\n"));
            Paragraph subtitle2 = new Paragraph(messages.get("pdf.spendBySupplier"), subtitleFont);
            document.add(subtitle2);
            document.add(new Paragraph("\n"));
            PdfPTable table = new PdfPTable(4);
            table.addCell(messages.get("pdf.colSupplier"));
            table.addCell(messages.get("pdf.colOrders"));
            table.addCell(messages.get("pdf.colSpend"));
            table.addCell(messages.get("pdf.colShare"));
            for (var p : report.getDespesaProveidors()) {
                table.addCell(p.getProveidor());
                table.addCell(String.valueOf(p.getNumComandes()));
                table.addCell(p.getDespesaTotal().toString());
                table.addCell(p.getPercentatge().toString());
            }
            document.add(table);

            // Taula de productes top
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("\n"));
            Paragraph subtitle1 = new Paragraph(messages.get("pdf.topProducts"), subtitleFont);
            document.add(subtitle1);
            document.add(new Paragraph("\n"));
            PdfPTable prodTable = new PdfPTable(3);
            prodTable.addCell(messages.get("pdf.colProduct"));
            prodTable.addCell(messages.get("pdf.colQuantity"));
            prodTable.addCell(messages.get("pdf.colSpend"));
            for (var p : report.getTopProductes()) {
                prodTable.addCell(p.getNomProducte());
                prodTable.addCell(p.getQuantitatTotal().toString());
                prodTable.addCell(p.getDespesaTotal().toString());
            }
            document.add(prodTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generant el PDF", e);
            throw new BadRequestException("error.pdf.failed");
        }
    }

    private DashboardResponseDTO mapToDashboardResponseDTO(int totalOrders,BigDecimal totalAmount,int pendingOrders) {
        return DashboardResponseDTO.builder()
                .totalComandes(totalOrders)
                .despesaComandes(totalAmount)
                .comandesPendents(pendingOrders)
                .build();
    }

    private ReportGlobalResponseDTO mapToReportResponseDTO(LocalDateTime dataInicial, LocalDateTime dataFinal, int totalComandes, BigDecimal despesaTotal, BigDecimal comandaMitjana, List<DespesaPerProveidorDTO> despesaProveidors, List<ProducteTopDTO> topProductes
    ) {
        return ReportGlobalResponseDTO.builder()
                .dataInicial(dataInicial)
                .dataFinal(dataFinal)
                .totalComandes(totalComandes)
                .despesaTotal(despesaTotal)
                .comandaMitjana(comandaMitjana)
                .despesaProveidors(despesaProveidors)
                .topProductes(topProductes)
                .build();
    }

}