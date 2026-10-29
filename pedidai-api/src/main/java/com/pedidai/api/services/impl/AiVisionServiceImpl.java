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
                  "unitInDocument": true,
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
              "AOVE Carbonell 5L" → "aceite de oliva virgen extra"; "Patata agria saco 25kg" → "patata". \
              Nunca cambies la especie por otra parecida: cigala, langostino, gamba y gambón son productos distintos \
              ("CIGALA Nº2" → "cigala"). Si el nombre está en catalán, tradúcelo ("LLOBARRO" → "lubina").
            - name: el nombre del producto tal como aparece, sin la cantidad ni el precio de la línea y sin lotes, \
              fechas ni códigos de trazabilidad ("BOQUERON PEQUEÑO 13/10/26" → "BOQUERON PEQUEÑO"); el calibre se \
              queda ("LUBINA 04/06").
            - unit: una de kg, g, l, ml, ud, caja, docena, garrafa, botella, paquete, bandeja, saco, lata, barril. \
              Es la unidad en la que se cuenta la cantidad y a la que corresponde unitPrice: si se venden envases \
              (ej.: "AOVE 5 L", 2 unidades a 32,50) la unidad es el envase (garrafa, botella, caja…), no "l" ni "kg". \
              "docena" solo si el documento dice "docena" o "doc"; un pack de 12 o de 24 es "caja". \
              Si el documento escribe la unidad, usa esa ("ud" son unidades). Si no la escribe: el pescado y el \
              marisco frescos van en kg, porque se venden por kilos, aunque la cantidad sea entera; la carne, la \
              fruta y la verdura sueltas, también, y una cantidad con decimales de peso ("1,100", "4,400") son kilos.
            - unitInDocument: true si la unidad aparece escrita en la línea o en la cabecera de la columna; false si \
              no aparece y la has deducido.
            - quantity es el número de la columna de cantidad, NO el formato que aparece en el nombre: en \
              "Cerveza lata 33 cl pack 24 | 3 | 18,50 | 55,50" la cantidad es 3 y la unidad "caja" (18,50 es el precio del pack de 24), no 24 ud; "pack 6", "saco 10 kg" \
              o "caja 12 ud" describen el envase. Comprueba que quantity × unitPrice ≈ subtotal. Si hay una columna \
              de bultos, cajas o piezas y otra de kilos o peso ("Cajas | Kg | €/Kg"), quantity son los kilos y unit \
              "kg": "MERLUZA | 2 | 18,500 | 12,40 | 229,40" → quantity 18.5, unitPrice 12.40.
            - unitPrice es el precio por unidad SIN IVA y con el descuento aplicado. Si solo aparece el importe de la \
              línea, calcula unitPrice = importe / cantidad. Si el documento no tiene precios (albarán sin valorar), \
              pon unitPrice a null.
            - subtotal es el importe de la línea tal como aparece en el documento.
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
              Ejemplos: "TOMÀQUET PERA CAT.1 5KG" → "tomate pera"; "Agua Font Vella 5L garrafa" → "agua mineral". \
              Nunca cambies la especie por otra parecida: cigala, langostino, gamba y gambón son productos distintos.
            - unit: una de kg, g, l, ml, ud, caja, docena, garrafa, botella, paquete, bandeja, saco, lata, barril; \
              la unidad a la que corresponde unitPrice ("€/kg" → kg; "caja 6 ud" a 12,00 → caja). Si la tarifa no \
              escribe la unidad, el pescado y el marisco frescos van en kg (se venden por kilos), y la carne, la \
              fruta y la verdura sueltas, también.
            - unitPrice es el precio por unidad SIN IVA. Si la tarifa indica que los precios llevan el IVA incluido, \
              quítalo. Si hay precios por tramos de cantidad, usa el de la compra más pequeña.
            - Los precios en España usan coma decimal ("1,55" = 1.55).
            - No inventes datos: si un campo no aparece, pon null.
            - Las fechas en formato YYYY-MM-DD (en España las fechas son día/mes/año).
            """;

    /** Mode AUTO: la IA decideix si és un albarà o factura o una tarifa i aplica les regles de cadascun. */
    private static final String AUTO_PROMPT = """
            Eres un asistente que lee documentos de proveedores de hostelería en España (castellano o catalán): \
            albaranes, facturas, tarifas, listas de precios y presupuestos. Recibirás el texto extraído del documento \
            (de un PDF, de una hoja de cálculo con las celdas separadas por " | ", o por OCR con posibles errores).
            Primero decide el tipo de documento (documentType):
            - "INVOICE": albarán, nota de entrega o factura. Registra una compra ya hecha: mercancía servida o \
              facturada a un cliente, con cantidades e importes (y normalmente un total).
            - "PRICE_LIST": tarifa, lista de precios, catálogo con precios, oferta o presupuesto. Son precios que el \
              proveedor ofrece, sin una compra hecha: no hay cantidades servidas ni importes por línea, o se indica \
              una validez ("válido hasta", "vigencia").
            Devuelve EXCLUSIVAMENTE un objeto JSON con esta forma:
            {
              "documentType": "INVOICE",
              "supplier": { "name": "...", "cif": "...", "address": "...", "phone": "..." },
              "invoice": { "number": "...", "date": "YYYY-MM-DD", "validUntil": "YYYY-MM-DD" },
              "products": [
                { "name": "nombre tal y como aparece", "genericName": "nombre genérico", "quantity": 10.0, "unit": "kg", \
            "unitInDocument": true, "unitPrice": 2.50, "ivaPercent": 10.0, "subtotal": 25.00 }
              ],
              "totals": { "base": 100.00, "iva": 10.00, "total": 110.00 }
            }
            Reglas para los dos tipos:
            - El proveedor es quien EMITE el documento, no el cliente que lo recibe. supplier.name es solo su nombre \
              comercial o razón social, sin el tipo de documento ("albarán", "factura", "tarifa", "presupuesto"), sin \
              número y sin dirección.
            - invoice.number: identificador del documento, sin la palabra que lo precede ("Albarán nº A-1234" → "A-1234").
            - genericName: SIEMPRE en castellano, minúsculas y singular, sin marca, calibre, formato ni envase. \
              Ejemplos: "TOMÀQUET PERA CAT.1 5KG" → "tomate pera"; "Agua Font Vella 5L garrafa" → "agua mineral"; \
              "AOVE Carbonell 5L" → "aceite de oliva virgen extra"; "Patata agria saco 25kg" → "patata". \
              Nunca cambies la especie por otra parecida: cigala, langostino, gamba y gambón son productos distintos \
              ("CIGALA Nº2" → "cigala"). Si el nombre está en catalán, tradúcelo ("LLOBARRO" → "lubina").
            - name: el nombre del producto tal como aparece, sin la cantidad ni el precio de la línea y sin lotes, \
              fechas ni códigos de trazabilidad ("BOQUERON PEQUEÑO 13/10/26" → "BOQUERON PEQUEÑO"); el calibre se \
              queda ("LUBINA 04/06").
            - unit: una de kg, g, l, ml, ud, caja, docena, garrafa, botella, paquete, bandeja, saco, lata, barril; \
              la unidad a la que corresponde unitPrice: si se venden envases (ej.: "AOVE 5 L", 2 unidades a 32,50) \
              la unidad es el envase (garrafa, botella, caja…), no "l" ni "kg". "docena" solo si el documento dice \
              "docena" o "doc"; un pack de 12 o de 24 es "caja". Si el documento escribe la unidad, \
              usa esa ("ud" son unidades). Si no la escribe: el pescado y el marisco frescos van en kg, porque se \
              venden por kilos, aunque la cantidad sea entera; la carne, la fruta y la verdura sueltas, también, y \
              una cantidad con decimales de peso ("1,100", "4,400") son kilos.
            - unitInDocument: true si la unidad aparece escrita en la línea o en la cabecera de la columna; false si \
              no aparece y la has deducido.
            - unitPrice es el precio por unidad SIN IVA y con el descuento aplicado. Los precios en España usan coma \
              decimal ("1,55" = 1.55).
            - No inventes datos: si un campo no aparece, pon null. No incluyas portes, envases retornables, \
              categorías, encabezados ni totales como productos.
            - Las fechas en formato YYYY-MM-DD (en España las fechas son día/mes/año).
            Si es INVOICE:
            - quantity es la cantidad servida: el número de la columna de cantidad, NO el formato que aparece en el \
              nombre ("Cerveza lata 33 cl pack 24 | 3 | 18,50 | 55,50" → quantity 3 y unit "caja", porque 18,50 es el precio del pack de 24; "pack 6" o "saco 10 kg" \
              describen el envase). Comprueba que quantity × unitPrice ≈ subtotal (el importe de la línea). Si hay \
              una columna de bultos, cajas o piezas y otra de kilos o peso ("Cajas | Kg | €/Kg"), quantity son los \
              kilos y unit "kg": "MERLUZA | 2 | 18,500 | 12,40 | 229,40" → quantity 18.5, unitPrice 12.40.
            - Si solo aparece el importe de la línea, calcula unitPrice = importe / cantidad. Si no tiene precios \
              (albarán sin valorar), unitPrice null. invoice.validUntil null.
            Si es PRICE_LIST:
            - quantity, subtotal y totals a null. Si los precios llevan el IVA incluido, quítalo. Si hay precios por \
              tramos de cantidad, usa el de la compra más pequeña.
            - invoice.date: fecha de la tarifa. invoice.validUntil: hasta cuándo valen los precios; si solo dice un \
              plazo ("válido 30 días"), súmalo a la fecha. Si no aparece, null.
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
        DocumentKind requested = kind != null ? kind : DocumentKind.INVOICE;
        // Si no se sap el tipus, es llegeix amb els límits de les tarifes, que són més llargues
        boolean large = requested != DocumentKind.INVOICE;
        String text = extractText(fileBytes, mediaType, large ? MAX_PRICE_LIST_PAGES : MAX_INVOICE_PAGES);
        int maxChars = large ? MAX_PRICE_LIST_CHARS : MAX_INVOICE_CHARS;
        boolean truncated = text.length() > maxChars;
        if (truncated) {
            text = text.substring(0, maxChars);
        }
        log.debug("Text del document: {} caràcters", text.length());
        log.trace("Text del document:\n{}", text);

        String prompt = switch (requested) {
            case PRICE_LIST -> PRICE_LIST_PROMPT;
            case AUTO -> AUTO_PROMPT;
            default -> INVOICE_PROMPT;
        };
        AiInvoiceDataDTO data = structureWithAi(text, prompt, large ? 180 : 60);
        DocumentKind detected = requested == DocumentKind.AUTO ? detectedKind(data) : requested;
        data.setDocumentType(detected.name());
        data.setTextTruncated(truncated);
        if (detected == DocumentKind.PRICE_LIST) {
            // En una tarifa no hi ha quantitats ni imports comprats
            if (data.getProducts() != null) {
                data.getProducts().forEach(p -> { p.setQuantity(null); p.setSubtotal(null); });
            }
        } else if (data.getInvoice() != null) {
            data.getInvoice().setValidUntil(null);
        }
        fillMissingUnitPrices(data);
        markUnitsWritten(data, text);
        cleanSupplierName(data);
        if (detected == DocumentKind.INVOICE) {
            fillMissingDocumentNumber(data, text);
        }
        return data;
    }

    private static final Pattern UNIT_WORD = Pattern.compile("\\b(kg|kgs|kilo|kilos|gr|grs|ud|uds|u|un|und|unid"
            + "|unidad|unidades|unitat|unitats|pz|pza|pzas|pieza|piezas|cj|caja|cajas|caixa|caixes|doc|docena|docenas"
            + "|lata|latas|bolsa|bolsas|bandeja|bandejas|paquete|paquetes|garrafa|garrafas|botella|botellas|saco|sacos)\\b");
    private static final Pattern TRAILING_QUANTITY = Pattern.compile("\\s+(\\d+(?:[.,]\\d+)?)\\s*([\\p{L}]+)\\.?$");
    /** «saco 10 kg», «caja 2 kg», «pack 24 ud»: la xifra és la mida de l'envàs, no la quantitat comprada. */
    private static final Pattern PACKAGE_BEFORE = Pattern.compile(
            "(?i)\\b(saco|caja|garrafa|bolsa|bandeja|lata|botella|malla|pack|paquete)$");

    /**
     * Decideix si la unitat de cada línia surt escrita al document, sense refiar-se del que diu la IA (diu que sí
     * encara que no hi sigui): ho és si a la línia del producte, tret del nom, es llegeix una unitat («20 uds»,
     * «5,200 | KG», o «2,300 kg x 10,50» a la línia següent d'un tiquet). Una columna «Ud.» a la capçalera no n'hi ha
     * prou: si a la línia no es llegeix (l'OCR ha fet «3|Kk6» de «3 | KG»), la unitat no hi és.
     * Si no es troba la línia, es deixa el que ha dit la IA.
     */
    static void markUnitsWritten(AiInvoiceDataDTO data, String text) {
        if (data.getProducts() == null || text == null) {
            return;
        }
        List<String> raw = text.lines().toList();
        List<String> lines = raw.stream().map(ProductNames::canonical).map(l -> l == null ? "" : l).toList();
        for (AiInvoiceDataDTO.ProductData p : data.getProducts()) {
            // L'OCR enganxa la cel·la de quantitat a la descripció i la IA la deixa al nom: «Tomate pera 15 kg»
            Matcher glued = p.getName() != null ? TRAILING_QUANTITY.matcher(p.getName()) : null;
            if (glued != null && glued.find() && p.getQuantity() != null
                    && new BigDecimal(glued.group(1).replace(',', '.')).compareTo(p.getQuantity()) == 0
                    && UNIT_WORD.matcher(ProductNames.canonical(glued.group(2))).matches()
                    && !PACKAGE_BEFORE.matcher(p.getName().substring(0, glued.start())).find()) {
                p.setName(p.getName().substring(0, glued.start()).trim());
                p.setUnitInDocument(true);
                continue;
            }
            String name = ProductNames.canonical(p.getName());
            if (name == null) {
                continue;
            }
            for (int i = 0; i < lines.size(); i++) {
                int at = lines.get(i).indexOf(name);
                if (at < 0) {
                    continue;
                }
                String rest = (lines.get(i).substring(0, at) + " " + lines.get(i).substring(at + name.length())).trim();
                String rawLine = raw.get(i);
                if (!rest.matches(".*\\d.*") && i + 1 < lines.size()) {
                    rest = lines.get(i + 1);
                    rawLine = raw.get(i + 1);
                }
                p.setUnitInDocument(UNIT_WORD.matcher(rest).find());
                quantityFromLine(p, rawLine);
                break;
            }
        }
    }

    private static final Pattern NUMBER = Pattern.compile("(?<![\\d.,])\\d+(?:[.,]\\d+)*");

    /**
     * Comprova la quantitat de la IA amb la mateixa línia del document: hi busca, començant per la dreta, tres
     * nombres seguits que quadrin (quantitat × preu = import). Si la IA ha posat una altra quantitat (sovint la
     * mida de l'envàs del nom: «SACO 25 KG | 2 | 14,00 | 28,00» llegit com 25 kg), mana la línia. Si ja coincideix,
     * no es toca res: el preu sense IVA que calcula la IA en un tiquet amb IVA inclòs es queda.
     */
    static void quantityFromLine(AiInvoiceDataDTO.ProductData p, String rawLine) {
        if (p.getQuantity() == null || rawLine == null) {
            return;
        }
        List<BigDecimal> n = NUMBER.matcher(rawLine).results().map(r -> spanishNumber(r.group()))
                .filter(java.util.Objects::nonNull).toList();
        for (int i = n.size() - 3; i >= 0; i--) {
            BigDecimal a = n.get(i), b = n.get(i + 1), c = n.get(i + 2);
            if (a.signum() <= 0 || b.signum() <= 0 || c.signum() <= 0) {
                continue;
            }
            BigDecimal tolerance = c.multiply(new BigDecimal("0.01")).max(new BigDecimal("0.02"));
            if (a.multiply(b).subtract(c).abs().compareTo(tolerance) > 0) {
                continue;
            }
            if (a.compareTo(p.getQuantity()) != 0) {
                log.info("Línia «{}»: quantitat {} de la IA canviada per {} × {} = {} de la línia",
                        p.getName(), p.getQuantity(), a, b, c);
                p.setQuantity(a);
                p.setUnitPrice(b);
                p.setSubtotal(c);
            }
            return;
        }
    }

    /** «1.234,50» → 1234.50; «18,500» → 18.5; «33.76» → 33.76 (l'OCR de vegades canvia la coma per un punt). */
    private static BigDecimal spanishNumber(String s) {
        try {
            if (s.contains(",")) {
                return new BigDecimal(s.replace(".", "").replace(',', '.'));
            }
            return s.chars().filter(ch -> ch == '.').count() > 1 ? new BigDecimal(s.replace(".", "")) : new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Tipus que ha triat la IA en mode AUTO. Si no ho diu clarament: és una tarifa si porta validesa
     * o cap línia té quantitat; si no, un albarà o factura.
     */
    static DocumentKind detectedKind(AiInvoiceDataDTO data) {
        String type = data.getDocumentType() != null ? data.getDocumentType().trim().toUpperCase(Locale.ROOT) : "";
        if (type.equals("PRICE_LIST")) {
            return DocumentKind.PRICE_LIST;
        }
        if (type.equals("INVOICE")) {
            return DocumentKind.INVOICE;
        }
        boolean hasValidity = data.getInvoice() != null && data.getInvoice().getValidUntil() != null
                && !data.getInvoice().getValidUntil().isBlank();
        boolean hasQuantities = data.getProducts() != null
                && data.getProducts().stream().anyMatch(p -> p.getQuantity() != null);
        return hasValidity || !hasQuantities ? DocumentKind.PRICE_LIST : DocumentKind.INVOICE;
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

    private AiInvoiceDataDTO structureWithAi(String text, String prompt, int timeoutSeconds) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", prompt),
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
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("choices").get(0).path("message").path("content").asText().trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("(?s)^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
            }
            log.debug("Resposta estructurada de la IA: {}", content);
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
