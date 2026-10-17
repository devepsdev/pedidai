package com.pedidai.api.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidai.api.dto.AiInvoiceDataDTO;
import com.pedidai.api.services.AiVisionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lectura d'albarans, factures i tarifes de proveïdors. El text s'extreu al servidor (text del PDF, full de
 * càlcul o OCR amb Tesseract) i només el TEXT s'envia a la IA (Mistral per defecte) per estructurar-lo.
 * El fitxer no surt mai del servidor.
 */
@Service
@Slf4j
public class AiVisionServiceImpl implements AiVisionService {

    /** Error funcional (document il·legible o IA no disponible) amb clau de missatge traduïble. */
    public static class InvoiceReadException extends RuntimeException {
        public InvoiceReadException(String key, Throwable cause) {
            super(key, cause);
        }
    }

    public static final String MEDIA_CSV = "text/csv";
    public static final String MEDIA_XLS = "application/vnd.ms-excel";
    public static final String MEDIA_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final int OCR_TIMEOUT_SECONDS = 90;
    private static final int MAX_INVOICE_CHARS = 20_000;
    private static final int MAX_PRICE_LIST_CHARS = 40_000;
    private static final int MAX_INVOICE_PAGES = 3;
    private static final int MAX_PRICE_LIST_PAGES = 8;
    private static final int MAX_SHEET_ROWS = 3_000;
    private static final int MAX_SHEET_COLUMNS = 40;
    /** Mínim de lletres i xifres perquè el text propi d'un PDF es consideri llegible (si no, OCR). */
    private static final int MIN_PDF_TEXT_CHARS = 40;

    private static final String INVOICE_PROMPT = """
            Eres un asistente que lee albaranes y facturas de proveedores de hostelería en España \
            (castellano o catalán). Recibirás el texto extraído del documento (por OCR, puede tener errores).
            Devuelve EXCLUSIVAMENTE un objeto JSON con esta forma:
            {
              "supplier": { "name": "...", "cif": "...", "address": "...", "phone": "..." },
              "invoice": { "number": "...", "date": "YYYY-MM-DD" },
              "products": [
                {
                  "name": "nombre tal y como aparece en el documento",
                  "genericName": "nombre genérico del producto",
                  "quantity": 10.0,
                  "unit": "kg",
                  "unitPrice": 2.50,
                  "ivaPercent": 10.0,
                  "subtotal": 25.00
                }
              ],
              "totals": { "base": 100.00, "iva": 10.00, "total": 110.00 }
            }
            Reglas:
            - El proveedor es quien EMITE el documento, no el cliente que lo recibe. supplier.name es solo su nombre \
              comercial o razón social, sin el tipo de documento ("albarán", "factura", "nota de entrega"), sin número \
              y sin dirección. Ej.: "DISTRIBUCIONES LÓPEZ    ALBARÁN Nº A-1234" → name "DISTRIBUCIONES LÓPEZ", number "A-1234".
            - invoice.number es el identificador del albarán o factura; suele aparecer junto a "Albarán nº", "Nº", \
              "Núm.", "Número", "Factura" o "Albarà núm.". Pon solo el identificador, sin la palabra que lo precede.
            - genericName: SIEMPRE en castellano, minúsculas y singular, sin marca, calibre, formato ni envase. \
              Ejemplos: "TOMÀQUET PERA CAT.1 5KG" → "tomate pera"; "Agua Font Vella 5L garrafa" → "agua mineral"; \
              "AOVE Carbonell 5L" → "aceite de oliva virgen extra"; "Patata agria saco 25kg" → "patata".
            - unit: una de kg, g, l, ml, ud, caja, docena, garrafa, botella, paquete, bandeja, saco, lata, barril. \
              Es la unidad en la que se cuenta la cantidad y a la que corresponde unitPrice: si se venden envases \
              (ej.: "AOVE 5 L", 2 unidades a 32,50) la unidad es el envase (garrafa, botella, caja…), no "l" ni "kg".
            - unitPrice es el precio por unidad SIN IVA y con el descuento aplicado. Si solo aparece el importe de la \
              línea, calcula unitPrice = importe / cantidad. Si el documento no tiene precios (albarán sin valorar), \
              pon unitPrice a null.
            - No inventes datos: si un campo no aparece, pon null. No incluyas portes, envases retornables ni totales como productos.
            - La fecha en formato YYYY-MM-DD (en España las fechas son día/mes/año).
            """;

    private static final String PRICE_LIST_PROMPT = """
            Eres un asistente que lee tarifas, listas de precios y presupuestos que los proveedores de hostelería \
            en España envían a bares y restaurantes (castellano o catalán). Recibirás el texto extraído del documento \
            (de un PDF, de una hoja de cálculo con las celdas separadas por " | ", o por OCR con posibles errores).
            Devuelve EXCLUSIVAMENTE un objeto JSON con esta forma:
            {
              "supplier": { "name": "...", "cif": "...", "address": "...", "phone": "..." },
              "invoice": { "number": "...", "date": "YYYY-MM-DD", "validUntil": "YYYY-MM-DD" },
              "products": [
                { "name": "nombre tal y como aparece", "genericName": "nombre genérico", "unit": "kg", "unitPrice": 2.50, "ivaPercent": 10.0 }
              ]
            }
            Reglas:
            - El proveedor es quien EMITE la tarifa, no el cliente. supplier.name es solo su nombre comercial o razón \
              social, sin "tarifa", "presupuesto" ni "lista de precios". Si no aparece, null.
            - invoice.number: número o referencia de la tarifa o presupuesto, si lo hay. invoice.date: fecha de la \
              tarifa. invoice.validUntil: fecha hasta la que son válidos los precios ("válido hasta", "vigencia", \
              "validesa"); si solo dice un plazo ("válido 30 días"), súmalo a la fecha. Si no aparece, null.
            - Un producto por cada artículo con precio. No incluyas categorías, encabezados, portes ni condiciones.
            - genericName: SIEMPRE en castellano, minúsculas y singular, sin marca, calibre, formato ni envase. \
              Ejemplos: "TOMÀQUET PERA CAT.1 5KG" → "tomate pera"; "Agua Font Vella 5L garrafa" → "agua mineral".
            - unit: una de kg, g, l, ml, ud, caja, docena, garrafa, botella, paquete, bandeja, saco, lata, barril; \
              la unidad a la que corresponde unitPrice ("€/kg" → kg; "caja 6 ud" a 12,00 → caja).
            - unitPrice es el precio por unidad SIN IVA. Si la tarifa indica que los precios llevan el IVA incluido, \
              quítalo. Si hay precios por tramos de cantidad, usa el de la compra más pequeña.
            - Los precios en España usan coma decimal ("1,55" = 1.55).
            - No inventes datos: si un campo no aparece, pon null.
            - Las fechas en formato YYYY-MM-DD (en España las fechas son día/mes/año).
            """;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String tesseractCommand;

    public AiVisionServiceImpl(
            ObjectMapper objectMapper,
            @Value("${ai.api.key:}") String apiKey,
            @Value("${ai.api.url:https://api.mistral.ai/v1}") String apiUrl,
            @Value("${ai.model:mistral-small-latest}") String model,
            @Value("${app.ocr.tesseract-command:tesseract}") String tesseractCommand) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.tesseractCommand = tesseractCommand;

        // Una tarifa llarga pot trigar més d'un minut a estructurar-se
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(180));
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Override
    public AiInvoiceDataDTO analyzeDocument(byte[] fileBytes, String mediaType, DocumentKind kind) {
        boolean priceList = kind == DocumentKind.PRICE_LIST;
        String text = extractText(fileBytes, mediaType, priceList ? MAX_PRICE_LIST_PAGES : MAX_INVOICE_PAGES);
        int maxChars = priceList ? MAX_PRICE_LIST_CHARS : MAX_INVOICE_CHARS;
        boolean truncated = text.length() > maxChars;
        if (truncated) {
            text = text.substring(0, maxChars);
        }
        log.debug("Text del document: {} caràcters", text.length());

        AiInvoiceDataDTO data = structureWithAi(text, priceList);
        data.setTextTruncated(truncated);
        fillMissingUnitPrices(data);
        cleanSupplierName(data);
        if (!priceList) {
            fillMissingDocumentNumber(data, text);
        }
        return data;
    }

    // ───────────────────────── Extracció del text ─────────────────────────

    private String extractText(byte[] bytes, String mediaType, int maxPages) {
        try {
            String text;
            if (MEDIA_CSV.equals(mediaType)) {
                text = csvToText(bytes);
            } else if (MEDIA_XLS.equals(mediaType) || MEDIA_XLSX.equals(mediaType)) {
                text = spreadsheetToText(bytes);
            } else if (mediaType != null && mediaType.contains("pdf")) {
                text = pdfToText(bytes, maxPages);
            } else {
                text = ocr(bytes);
            }
            text = text == null ? "" : text.trim();
            if (text.length() < 20) {
                throw new InvoiceReadException("error.invoice.unreadable", null);
            }
            return text;
        } catch (InvoiceReadException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error llegint el document ({})", mediaType, e);
            throw new InvoiceReadException("error.invoice.unreadable", e);
        }
    }

    /** PDF digital: el seu text directament; PDF escanejat: OCR pàgina a pàgina. */
    private String pdfToText(byte[] pdfBytes, int maxPages) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            int pages = Math.min(document.getNumberOfPages(), maxPages);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(pages);
            String embedded = stripper.getText(document);
            if (embedded.replaceAll("[^\\p{L}\\p{N}]", "").length() >= MIN_PDF_TEXT_CHARS) {
                return embedded;
            }
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 250);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", out);
                text.append(ocr(out.toByteArray())).append("\n\n");
            }
            return text.toString();
        }
    }

    /** Full de càlcul (.xlsx o .xls): una línia per fila amb les cel·les separades per " | ". */
    static String spreadsheetToText(byte[] bytes) throws IOException {
        DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("es-ES"));
        StringBuilder text = new StringBuilder();
        int rows = 0;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            for (int s = 0; s < workbook.getNumberOfSheets() && rows < MAX_SHEET_ROWS; s++) {
                if (workbook.isSheetHidden(s) || workbook.isSheetVeryHidden(s)) {
                    continue;
                }
                Sheet sheet = workbook.getSheetAt(s);
                text.append("## ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    int last = Math.min(row.getLastCellNum(), MAX_SHEET_COLUMNS);
                    for (int c = 0; c < last; c++) {
                        String value = cellText(row.getCell(c), formatter);
                        if (!value.isBlank()) {
                            cells.add(value.trim().replaceAll("\\s+", " "));
                        }
                    }
                    if (!cells.isEmpty()) {
                        text.append(String.join(" | ", cells)).append('\n');
                        if (++rows >= MAX_SHEET_ROWS) {
                            break;
                        }
                    }
                }
            }
        }
        return text.toString();
    }

    /** Valor visible d'una cel·la; de les fórmules, el darrer resultat desat (no es recalculen). */
    private static String cellText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() != CellType.FORMULA) {
            return formatter.formatCellValue(cell);
        }
        return switch (cell.getCachedFormulaResultType()) {
            case NUMERIC -> formatter.formatRawCellContents(cell.getNumericCellValue(),
                    cell.getCellStyle().getDataFormat(), cell.getCellStyle().getDataFormatString());
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    /** CSV en UTF-8 o, si no ho és, en la codificació d'Excel a Windows. */
    static String csvToText(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.indexOf('\uFFFD') >= 0) {
            text = new String(bytes, Charset.forName("windows-1252"));
        }
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    private String ocr(byte[] imageBytes) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Path tmp = Path.of(System.getProperty("java.io.tmpdir"));
        Path input = tmp.resolve("invoice_" + id + ".png");
        Path outputBase = tmp.resolve("invoice_" + id + "_out");
        Path outputTxt = Path.of(outputBase + ".txt");
        Path processLog = tmp.resolve("invoice_" + id + "_log.txt");

        try {
            Files.write(input, imageBytes);

            Process process = new ProcessBuilder(tesseractCommand, input.toString(), outputBase.toString(), "-l", "spa+cat")
                    .redirectErrorStream(true)
                    .redirectOutput(processLog.toFile())
                    .start();
            if (!process.waitFor(OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new InvoiceReadException("error.invoice.ocrFailed", null);
            }
            if (process.exitValue() != 0 || !Files.exists(outputTxt)) {
                log.error("Tesseract ha fallat (exit={}): {}", process.exitValue(), Files.readString(processLog, StandardCharsets.UTF_8));
                throw new InvoiceReadException("error.invoice.ocrFailed", null);
            }
            return Files.readString(outputTxt).trim();

        } catch (InvoiceReadException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvoiceReadException("error.invoice.ocrFailed", e);
        } catch (Exception e) {
            log.error("Error executant l'OCR", e);
            throw new InvoiceReadException("error.invoice.ocrFailed", e);
        } finally {
            try { Files.deleteIfExists(input); } catch (IOException ignored) { }
            try { Files.deleteIfExists(outputTxt); } catch (IOException ignored) { }
            try { Files.deleteIfExists(processLog); } catch (IOException ignored) { }
        }
    }

    // ───────────────────────── IA ─────────────────────────

    private AiInvoiceDataDTO structureWithAi(String text, boolean priceList) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", priceList ? PRICE_LIST_PROMPT : INVOICE_PROMPT),
                        Map.of("role", "user", "content", "Texto del documento:\n\n" + text)),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0,
                "stream", false
        );

        try {
            String responseJson = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(priceList ? 180 : 60))
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("choices").get(0).path("message").path("content").asText().trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("(?s)^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
            }
            return objectMapper.readValue(content, AiInvoiceDataDTO.class);

        } catch (Exception e) {
            log.error("Error de la IA en estructurar el document: {}", e.getMessage());
            throw new InvoiceReadException("error.invoice.aiFailed", e);
        }
    }

    // ───────────────────────── Neteja de la resposta ─────────────────────────

    /** Paraules del tipus de document que la IA de vegades enganxa al nom del proveïdor. */
    private static final Pattern DOCUMENT_WORDS = Pattern.compile(
            "(?iU)\\b(albar[aáà]n(es)?|albar[aà]ns?|factura|facturas|factures|tiquet|ticket|nota de entrega"
                    + "|tarifa|tarifas|tarifes|presupuesto|pressupost|lista de precios|llista de preus)\\b");

    /** "Albarán nº A-1234", "Factura núm. 2026/15", "Nº: 8812"… (només si la IA no ha trobat el número). */
    private static final Pattern DOCUMENT_NUMBER = Pattern.compile(
            "(?iU)\\b(?:albar[aáà]n|albar[aà]|factura|n[uú]m(?:ero)?\\.?|n[ºo°]\\.?)\\s*(?:n[ºo°]\\.?|n[uú]m(?:ero)?\\.?)?"
                    + "\\s*[:#.]?\\s*([A-Z0-9][A-Z0-9/\\-.]{1,30})");

    /** Treu del nom del proveïdor paraules com "albarán", "factura" o "tarifa" ("DISTRIBUCIONES LÓPEZ ALBARÁN"). */
    static void cleanSupplierName(AiInvoiceDataDTO data) {
        if (data.getSupplier() == null || data.getSupplier().getName() == null) {
            return;
        }
        String original = data.getSupplier().getName();
        if (!DOCUMENT_WORDS.matcher(original).find()) {
            return;
        }
        String cleaned = DOCUMENT_WORDS.matcher(original).replaceAll(" ")
                .replaceAll("\\s+", " ").replaceAll("^[\\s,.:;\\-]+|[\\s,:;\\-]+$", "");
        if (!cleaned.isBlank()) {
            data.getSupplier().setName(cleaned);
        }
    }

    /** Si la IA no ha trobat el número del document, el busca al text de l'OCR. */
    static void fillMissingDocumentNumber(AiInvoiceDataDTO data, String ocrText) {
        if (ocrText == null || (data.getInvoice() != null && data.getInvoice().getNumber() != null
                && !data.getInvoice().getNumber().isBlank())) {
            return;
        }
        String number = findDocumentNumber(ocrText);
        if (number == null) {
            return;
        }
        if (data.getInvoice() == null) {
            data.setInvoice(new AiInvoiceDataDTO.InvoiceData());
        }
        data.getInvoice().setNumber(number);
    }

    static String findDocumentNumber(String text) {
        Matcher m = DOCUMENT_NUMBER.matcher(text);
        while (m.find()) {
            String candidate = m.group(1).replaceAll("[./\\-]+$", "");
            boolean isDate = candidate.matches("\\d{1,2}[/.\\-]\\d{1,2}[/.\\-]\\d{2,4}");
            if (candidate.length() >= 3 && candidate.matches(".*\\d.*") && !isDate) {
                return candidate;
            }
        }
        return null;
    }

    /** Si la IA no dona preu unitari però sí import i quantitat, el calcula. */
    private static void fillMissingUnitPrices(AiInvoiceDataDTO data) {
        if (data == null || data.getProducts() == null) {
            return;
        }
        for (AiInvoiceDataDTO.ProductData p : data.getProducts()) {
            if (p.getUnitPrice() == null && p.getSubtotal() != null && p.getQuantity() != null
                    && p.getQuantity().signum() > 0) {
                p.setUnitPrice(p.getSubtotal().divide(p.getQuantity(), 4, RoundingMode.HALF_UP));
            }
            if (p.getUnitPrice() != null && p.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                p.setUnitPrice(null);
            }
        }
    }
}
