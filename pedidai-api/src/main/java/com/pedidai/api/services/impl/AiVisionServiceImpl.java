package com.pedidai.api.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidai.api.dto.AiInvoiceDataDTO;
import com.pedidai.api.services.AiVisionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Lectura d'albarans i factures: l'OCR (Tesseract) s'executa al servidor i només el TEXT
 * extret s'envia a DeepSeek per estructurar-lo. La imatge no surt mai del servidor.
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

    private static final int OCR_TIMEOUT_SECONDS = 90;
    private static final int MAX_OCR_CHARS = 20_000;

    private static final String SYSTEM_PROMPT = """
            Eres un asistente que lee albaranes y facturas de proveedores de hostelería en España \
            (castellano o catalán). Recibirás el texto extraído por OCR, que puede tener errores.
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
            - El proveedor es quien EMITE el documento, no el cliente que lo recibe.
            - genericName: SIEMPRE en castellano, minúsculas y singular, sin marca, calibre, formato ni envase. \
              Ejemplos: "TOMÀQUET PERA CAT.1 5KG" → "tomate pera"; "Agua Font Vella 5L garrafa" → "agua mineral"; \
              "AOVE Carbonell 5L" → "aceite de oliva virgen extra"; "Patata agria saco 25kg" → "patata".
            - unit: una de kg, g, l, ml, ud, caja, docena, garrafa, botella, paquete, bandeja, saco, lata, barril.
            - unitPrice es el precio por unidad SIN IVA y con el descuento aplicado. Si solo aparece el importe de la \
              línea, calcula unitPrice = importe / cantidad. Si el documento no tiene precios (albarán sin valorar), \
              pon unitPrice a null.
            - No inventes datos: si un campo no aparece, pon null. No incluyas portes, envases retornables ni totales como productos.
            - La fecha en formato YYYY-MM-DD (en España las fechas son día/mes/año).
            """;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String tesseractCommand;

    public AiVisionServiceImpl(
            ObjectMapper objectMapper,
            @Value("${deepseek.api.key}") String apiKey,
            @Value("${deepseek.api.url:https://api.deepseek.com}") String apiUrl,
            @Value("${deepseek.model:deepseek-v4-flash}") String model,
            @Value("${app.ocr.tesseract-command:tesseract}") String tesseractCommand) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.tesseractCommand = tesseractCommand;

        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(60));
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Override
    public AiInvoiceDataDTO analyzeInvoice(byte[] imageBytes, String mediaType) {
        String ocrText = extractTextWithTesseract(imageBytes, mediaType);
        log.debug("OCR: {} caràcters", ocrText.length());
        AiInvoiceDataDTO data = structureWithDeepSeek(ocrText);
        fillMissingUnitPrices(data);
        return data;
    }

    private String extractTextWithTesseract(byte[] imageBytes, String mediaType) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Path tmp = Path.of(System.getProperty("java.io.tmpdir"));
        Path input = tmp.resolve("invoice_" + id + ".png");
        Path outputBase = tmp.resolve("invoice_" + id + "_out");
        Path outputTxt = Path.of(outputBase + ".txt");
        Path processLog = tmp.resolve("invoice_" + id + "_log.txt");

        try {
            byte[] png = mediaType != null && mediaType.contains("pdf") ? renderPdfFirstPageToPng(imageBytes) : imageBytes;
            Files.write(input, png);

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

            String text = Files.readString(outputTxt).trim();
            if (text.length() < 20) {
                throw new InvoiceReadException("error.invoice.unreadable", null);
            }
            return text.length() > MAX_OCR_CHARS ? text.substring(0, MAX_OCR_CHARS) : text;

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

    private byte[] renderPdfFirstPageToPng(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 250);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        }
    }

    private AiInvoiceDataDTO structureWithDeepSeek(String ocrText) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", "Texto OCR del documento:\n\n" + ocrText)),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0,
                "stream", false,
                "thinking", Map.of("type", "disabled")
        );

        try {
            String responseJson = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("choices").get(0).path("message").path("content").asText().trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("(?s)^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
            }
            return objectMapper.readValue(content, AiInvoiceDataDTO.class);

        } catch (Exception e) {
            log.error("Error de DeepSeek en estructurar l'albarà: {}", e.getMessage());
            throw new InvoiceReadException("error.invoice.aiFailed", e);
        }
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
