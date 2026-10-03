package com.pedidai.api.services.impl;

import com.pedidai.api.dto.AiInvoiceDataDTO;
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
    @DisplayName("si la IA ja ha trobat el número, no el canvia")
    void keepsNumberFoundByAi() {
        AiInvoiceDataDTO d = data("X", "H-55120");
        AiVisionServiceImpl.fillMissingDocumentNumber(d, "Albarán nº 999");
        assertThat(d.getInvoice().getNumber()).isEqualTo("H-55120");
    }
}
