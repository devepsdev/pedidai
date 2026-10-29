package com.pedidai.api.services.impl;

import com.pedidai.api.dto.AiInvoiceDataDTO;
import com.pedidai.api.services.AiVisionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiVisionServiceImpl: neteja de la resposta de la IA")
class AiVisionServiceImplTest {

    private static AiInvoiceDataDTO data(String supplierName, String number) {
        AiInvoiceDataDTO d = new AiInvoiceDataDTO();
        AiInvoiceDataDTO.SupplierData s = new AiInvoiceDataDTO.SupplierData();
        s.setName(supplierName);
        d.setSupplier(s);
        AiInvoiceDataDTO.InvoiceData i = new AiInvoiceDataDTO.InvoiceData();
        i.setNumber(number);
        d.setInvoice(i);
        return d;
    }

    @Test
    @DisplayName("treu «albarán» o «factura» del nom del proveïdor")
    void cleansDocumentWordsFromSupplierName() {
        AiInvoiceDataDTO d = data("DISTRIBUCIONES LÓPEZ ALBARÁN", null);
        AiVisionServiceImpl.cleanSupplierName(d);
        assertThat(d.getSupplier().getName()).isEqualTo("DISTRIBUCIONES LÓPEZ");

        AiInvoiceDataDTO f = data("Factura - Fruites del Maresme SL", null);
        AiVisionServiceImpl.cleanSupplierName(f);
        assertThat(f.getSupplier().getName()).isEqualTo("Fruites del Maresme SL");
    }

    @Test
    @DisplayName("no toca un nom que no porta paraules de document")
    void keepsNormalSupplierName() {
        AiInvoiceDataDTO d = data("Horeca Ejemplo S.A.", null);
        AiVisionServiceImpl.cleanSupplierName(d);
        assertThat(d.getSupplier().getName()).isEqualTo("Horeca Ejemplo S.A.");
    }

    @Test
    @DisplayName("si la IA no troba el número, el treu de l'OCR")
    void fillsMissingNumberFromOcr() {
        String ocr = """
                DISTRIBUCIONES LÓPEZ    ALBARÁN Nº A-2026-0815
                Teléfono 930 000 000
                Fecha: 01/10/2026
                """;
        AiInvoiceDataDTO d = data("DISTRIBUCIONES LÓPEZ", null);
        AiVisionServiceImpl.fillMissingDocumentNumber(d, ocr);
        assertThat(d.getInvoice().getNumber()).isEqualTo("A-2026-0815");
    }

    @Test
    @DisplayName("no confon telèfons ni dates amb el número del document")
    void ignoresPhonesAndDates() {
        assertThat(AiVisionServiceImpl.findDocumentNumber("Teléfono 930000000\nFactura 01/10/2026\nCliente 4521")).isNull();
        assertThat(AiVisionServiceImpl.findDocumentNumber("Albarà núm. 2026/155")).isEqualTo("2026/155");
        assertThat(AiVisionServiceImpl.findDocumentNumber("Nº: 8812")).isEqualTo("8812");
    }

    @Test
    @DisplayName("treu «tarifa» del nom del proveïdor")
    void cleansPriceListWordFromSupplierName() {
        AiInvoiceDataDTO d = data("Tarifa Fruites del Maresme SL", null);
        AiVisionServiceImpl.cleanSupplierName(d);
        assertThat(d.getSupplier().getName()).isEqualTo("Fruites del Maresme SL");
    }

    @Test
    @DisplayName("un Excel es converteix en una línia per fila i se salten els fulls ocults")
    void spreadsheetToText() throws Exception {
        byte[] bytes;
        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); var out = new java.io.ByteArrayOutputStream()) {
            var sheet = wb.createSheet("Tarifa");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Producto");
            header.createCell(1).setCellValue("Precio €/kg");
            var row = sheet.createRow(2);
            row.createCell(0).setCellValue("Tomate pera");
            row.createCell(1).setCellValue(1.55);
            wb.createSheet("Interno").createRow(0).createCell(0).setCellValue("coste interno");
            wb.setSheetHidden(1, true);
            wb.write(out);
            bytes = out.toByteArray();
        }

        String text = AiVisionServiceImpl.spreadsheetToText(bytes);

        assertThat(text).contains("## Tarifa").contains("Producto | Precio €/kg").contains("Tomate pera | 1,55")
                .doesNotContain("coste interno");
    }

    @Test
    @DisplayName("un CSV desat amb Excel a Windows es llegeix amb els accents correctes")
    void csvInWindowsEncoding() {
        byte[] latin = "Tomàquet pera;1,55\n".getBytes(java.nio.charset.Charset.forName("windows-1252"));
        byte[] utf8Bom = "﻿Agua mineral;0,40\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(AiVisionServiceImpl.csvToText(latin)).isEqualTo("Tomàquet pera;1,55\n");
        assertThat(AiVisionServiceImpl.csvToText(utf8Bom)).isEqualTo("Agua mineral;0,40\n");
    }

    @Test
    @DisplayName("en mode AUTO es fa cas del tipus que diu la IA i, si no en diu cap, es dedueix")
    void detectsDocumentKind() {
        AiInvoiceDataDTO saysPriceList = data("X", null);
        saysPriceList.setDocumentType("price_list");
        assertThat(AiVisionServiceImpl.detectedKind(saysPriceList)).isEqualTo(AiVisionService.DocumentKind.PRICE_LIST);

        AiInvoiceDataDTO withQuantities = data("X", null);
        AiInvoiceDataDTO.ProductData line = new AiInvoiceDataDTO.ProductData();
        line.setQuantity(java.math.BigDecimal.TEN);
        withQuantities.setProducts(java.util.List.of(line));
        assertThat(AiVisionServiceImpl.detectedKind(withQuantities)).isEqualTo(AiVisionService.DocumentKind.INVOICE);

        withQuantities.getInvoice().setValidUntil("2026-12-31");
        assertThat(AiVisionServiceImpl.detectedKind(withQuantities)).isEqualTo(AiVisionService.DocumentKind.PRICE_LIST);

        AiInvoiceDataDTO withoutQuantities = data("X", null);
        withoutQuantities.setProducts(java.util.List.of(new AiInvoiceDataDTO.ProductData()));
        assertThat(AiVisionServiceImpl.detectedKind(withoutQuantities)).isEqualTo(AiVisionService.DocumentKind.PRICE_LIST);
    }

    private static AiInvoiceDataDTO lines(String... names) {
        AiInvoiceDataDTO d = data("X", null);
        d.setProducts(java.util.Arrays.stream(names).map(n -> {
            AiInvoiceDataDTO.ProductData p = new AiInvoiceDataDTO.ProductData();
            p.setName(n);
            p.setUnitInDocument(true);
            return p;
        }).toList());
        return d;
    }

    @Test
    @DisplayName("si a la línia no hi ha cap unitat ni la capçalera en té, no està escrita encara que la IA digui que sí")
    void unitNotWrittenInDocument() {
        String text = """
                Descripción Cantidad Precio Importe
                CALAMAR NACIONAL 10 14,50 145,00
                PEREJIL MANOJO 5 0,60 3,00
                GAMBÓN ARGENTINO 10/20 CAJA 2 KG 3 24,90 74,70
                """;
        AiInvoiceDataDTO d = lines("CALAMAR NACIONAL", "PEREJIL MANOJO", "GAMBÓN ARGENTINO 10/20 CAJA 2 KG");

        AiVisionServiceImpl.markUnitsWritten(d, text);

        assertThat(d.getProducts()).allSatisfy(p -> assertThat(p.getUnitInDocument()).isFalse());
    }

    @Test
    @DisplayName("una columna d'unitats a la capçalera no n'hi ha prou: cal llegir-la a la línia («3|Kk6» no és cap unitat)")
    void headerUnitIsNotEnough() {
        String text = """
                Artículo Cant. | Ud. Precio Importe
                NAVAJA 3|Kk6 19,50 58,50
                ERIZO DE MAR 30 | UD 0,90 27,00
                """;
        AiInvoiceDataDTO d = lines("NAVAJA", "ERIZO DE MAR");

        AiVisionServiceImpl.markUnitsWritten(d, text);

        assertThat(d.getProducts().get(0).getUnitInDocument()).isFalse();
        assertThat(d.getProducts().get(1).getUnitInDocument()).isTrue();
    }

    @Test
    @DisplayName("la unitat és escrita si surt a la mateixa línia, fora del nom, o a la següent d'un tiquet")
    void unitWrittenInDocument() {
        String perLine = """
                Producto Unid. Precio Total
                DORADA RACION 400/600 GR 20 uds 4,20 84,00
                SEPIA
                2,300 kg x 10,50 24,15
                """;
        AiInvoiceDataDTO d = lines("DORADA RACION 400/600 GR", "SEPIA");
        d.getProducts().forEach(p -> p.setUnitInDocument(false));
        AiVisionServiceImpl.markUnitsWritten(d, perLine);
        assertThat(d.getProducts().get(0).getUnitInDocument()).isTrue();
        assertThat(d.getProducts().get(1).getUnitInDocument()).isTrue();
    }

    @Test
    @DisplayName("si el nom acaba amb la quantitat i la unitat de la línia, se'n treuen i la unitat compta com a escrita")
    void quantityAndUnitGluedToName() {
        String text = """
                Descripción Cantidad Precio Importe
                Tomate pera cat. 1 15 kg 1,95 29,25
                GALLO SAN PEDRO 10 ud 21,90 219,00
                Café en grano 1 kg 2 16,50 33,00
                """;
        AiInvoiceDataDTO d = lines("Tomate pera cat. 1 15 kg", "GALLO SAN PEDRO 10 ud", "Café en grano 1 kg");
        d.getProducts().get(0).setQuantity(new java.math.BigDecimal("15"));
        d.getProducts().get(1).setQuantity(new java.math.BigDecimal("10.0"));
        d.getProducts().get(2).setQuantity(new java.math.BigDecimal("2"));
        d.getProducts().forEach(p -> p.setUnitInDocument(false));

        AiVisionServiceImpl.markUnitsWritten(d, text);

        assertThat(d.getProducts().get(0).getName()).isEqualTo("Tomate pera cat. 1");
        assertThat(d.getProducts().get(0).getUnitInDocument()).isTrue();
        assertThat(d.getProducts().get(1).getName()).isEqualTo("GALLO SAN PEDRO");
        assertThat(d.getProducts().get(1).getUnitInDocument()).isTrue();
        assertThat(d.getProducts().get(2).getName()).isEqualTo("Café en grano 1 kg");

        // «saco 10 kg» és la mida de l'envàs, encara que la IA hagi posat 10 kg de quantitat
        AiInvoiceDataDTO cebolla = lines("Cebolla blanca saco 10 kg");
        cebolla.getProducts().get(0).setQuantity(new java.math.BigDecimal("10"));
        AiVisionServiceImpl.markUnitsWritten(cebolla, "Cebolla blanca saco 10 kg 1 saco 9,60 9,60\n");
        assertThat(cebolla.getProducts().get(0).getName()).isEqualTo("Cebolla blanca saco 10 kg");
    }

    private static AiInvoiceDataDTO.ProductData line(AiInvoiceDataDTO d, int i, String q, String p, String s) {
        AiInvoiceDataDTO.ProductData pd = d.getProducts().get(i);
        pd.setQuantity(new java.math.BigDecimal(q));
        pd.setUnitPrice(new java.math.BigDecimal(p));
        pd.setSubtotal(new java.math.BigDecimal(s));
        return pd;
    }

    @Test
    @DisplayName("si la quantitat de la IA no és la de la línia, mana la línia: la terna quantitat × preu = import")
    void quantityFromTheDocumentLine() {
        String text = """
                PATATAAGRIA SACO 25 KG 2 14,00 28,00
                MERLUZA PINCHO 2-3 KG 2 18,500 12,40 229,40
                SEPIA
                2,300 kg x 10,50 24,15
                """;
        AiInvoiceDataDTO d = lines("PATATAAGRIA SACO 25 KG", "MERLUZA PINCHO 2-3 KG", "SEPIA");
        var patata = line(d, 0, "25", "1.12", "28.00");
        var merluza = line(d, 1, "2", "114.70", "229.40");
        var sepia = line(d, 2, "2.3", "9.5455", "21.95");

        AiVisionServiceImpl.markUnitsWritten(d, text);

        assertThat(patata.getQuantity()).isEqualByComparingTo("2");
        assertThat(patata.getUnitPrice()).isEqualByComparingTo("14.00");
        assertThat(merluza.getQuantity()).isEqualByComparingTo("18.5");
        assertThat(merluza.getUnitPrice()).isEqualByComparingTo("12.40");
        // Si la quantitat ja és la de la línia, no es toca (el preu sense IVA del tiquet es queda)
        assertThat(sepia.getUnitPrice()).isEqualByComparingTo("9.5455");
    }

    @Test
    @DisplayName("si la IA ja ha trobat el número, no el canvia")
    void keepsNumberFoundByAi() {
        AiInvoiceDataDTO d = data("X", "H-55120");
        AiVisionServiceImpl.fillMissingDocumentNumber(d, "Albarán nº 999");
        assertThat(d.getInvoice().getNumber()).isEqualTo("H-55120");
    }
}
