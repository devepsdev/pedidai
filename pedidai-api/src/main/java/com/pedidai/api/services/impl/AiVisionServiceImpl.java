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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AiVisionServiceImpl implements AiVisionService {

    private static final String SYSTEM_PROMPT = """
            Eres un asistente especializado en leer facturas de proveedores. \
            Te voy a dar el texto extraído de una factura por OCR. \
            Analiza el texto y extrae los datos en formato JSON estricto:
            {
              "supplier": {
                "name": "nombre del proveedor",
                "cif": "CIF/NIF",
                "address": "dirección si aparece",
                "phone": "teléfono si aparece"
              },
              "invoice": {
                "number": "número de factura",
                "date": "YYYY-MM-DD"
              },
              "products": [
                {
                  "name": "nombre del producto",
                  "quantity": 10.0,
                  "unit": "kg|l|ud|caja",
                  "unitPrice": 2.50,
                  "ivaPercent": 10.0,
                  "subtotal": 25.00
                }
              ],
              "totals": {
                "base": 100.00,
                "iva": 10.00,
                "total": 110.00
              }
            }
            Reglas:
            - Devuelve SOLO JSON válido, sin markdown ni explicaciones
            - Si no puedes identificar un campo, pon null
            - Precios siempre con 2 decimales
            - Normaliza unidades: kg, g, l, ml, ud, caja, pack
            - Si el texto OCR tiene errores tipográficos, intenta corregirlos
            - Si hay varias columnas mal alineadas, usa el contexto para deducir qué es cantidad, precio, etc.
            """;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public AiVisionServiceImpl(
            ObjectMapper objectMapper,
            @Value("${deepseek.api.key}") String apiKey,
            @Value("${deepseek.api.url:https://api.deepseek.com}") String apiUrl,
            @Value("${deepseek.model:deepseek-v4-flash}") String model) {
        this.objectMapper = objectMapper;
        this.model = model;

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30));

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
        // Paso 1: Extraer texto con Tesseract OCR
        String ocrText = extractTextWithTesseract(imageBytes, mediaType);
        log.debug("Tesseract extrajo {} caracteres", ocrText.length());

        // Paso 2: Estructurar el texto con DeepSeek (chat normal, sin vision)
        return structureWithDeepSeek(ocrText);
    }

    private String extractTextWithTesseract(byte[] imageBytes, String mediaType) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Path tempInput = Path.of(System.getProperty("java.io.tmpdir"), "invoice_" + id + ".png");
        Path tempOutputBase = Path.of(System.getProperty("java.io.tmpdir"), "invoice_" + id + "_out");
        Path tempOutputTxt = Path.of(tempOutputBase + ".txt");

        try {
            // Si es PDF, renderizar la primera página a PNG con PDFBox
            byte[] pngBytes = (mediaType != null && mediaType.contains("pdf"))
                    ? renderPdfFirstPageToPng(imageBytes)
                    : imageBytes;

            Files.write(tempInput, pngBytes);

            ProcessBuilder pb = new ProcessBuilder(
                    "tesseract", tempInput.toString(), tempOutputBase.toString(), "-l", "spa+cat"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String tesseractLog = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Tesseract falló (exit=" + exitCode + "): " + tesseractLog);
            }
            if (!Files.exists(tempOutputTxt)) {
                throw new RuntimeException("Tesseract no generó el archivo de salida. Log: " + tesseractLog);
            }

            String text = Files.readString(tempOutputTxt).trim();
            if (text.isEmpty()) {
                throw new RuntimeException("Tesseract no extrajo texto. Verifica que la imagen sea legible.");
            }

            log.debug("OCR completado. Primeros 200 chars: {}", text.substring(0, Math.min(200, text.length())));
            return text;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error ejecutando Tesseract OCR: " + e.getMessage(), e);
        } finally {
            try { Files.deleteIfExists(tempInput); } catch (IOException ignored) {}
            try { Files.deleteIfExists(tempOutputTxt); } catch (IOException ignored) {}
        }
    }

    private byte[] renderPdfFirstPageToPng(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 200);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        }
    }

    private AiInvoiceDataDTO structureWithDeepSeek(String ocrText) {
        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", SYSTEM_PROMPT
        );
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", "Texto extraído de la factura:\n\n" + ocrText
        );
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(systemMessage, userMessage),
                "stream", false
        );

        try {
            String responseJson = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new RuntimeException(
                                            "DeepSeek API error " + response.statusCode() + ": " + body))
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            content = content.trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("(?s)^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
            }

            return objectMapper.readValue(content, AiInvoiceDataDTO.class);

        } catch (Exception e) {
            log.error("DeepSeek API error: {}", e.getMessage(), e);
            throw new RuntimeException("Error al estructurar el texto con DeepSeek: " + e.getMessage(), e);
        }
    }
}
