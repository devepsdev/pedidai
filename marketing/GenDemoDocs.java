import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Documentos de ejemplo para los vídeos: foto de albarán de Distribuciones López, tarifa en Excel de
 * Huerta del Valle y factura en PDF de Bebidas Costa. Uso: java -cp <classpath de la API> GenDemoDocs.java <carpeta>
 */
public class GenDemoDocs {
    static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void main(String[] args) throws Exception {
        String dir = args[0];
        albaran(dir + "/albaran-lopez.jpg");
        tarifa(dir + "/tarifa-huerta-del-valle.xlsx");
        factura(dir + "/factura-bebidas-costa.pdf");
        System.out.println("ok");
    }

    /** «Foto» de un albarán: papel algo girado sobre una mesa, luz desigual y ruido suave. */
    static void albaran(String out) throws Exception {
        int w = 1500, h = 1900;
        BufferedImage paper = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = paper.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(250, 249, 244));
        g.fillRect(0, 0, w, h);
        Color tinta = new Color(28, 30, 38);
        Color azul = new Color(24, 70, 140);

        g.setColor(azul);
        g.fillRoundRect(90, 90, 110, 110, 24, 24);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 64));
        g.drawString("DL", 104, 168);
        g.setColor(tinta);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("DISTRIBUCIONES LÓPEZ S.L.", 230, 140);
        g.setFont(new Font("Arial", Font.PLAIN, 28));
        g.drawString("CIF B-12345678 · Pol. Ind. Sur, nave 4 · 08940 Cornellà", 230, 185);
        g.drawString("Tel. 930 000 000 · pedidos@dlopez.test", 230, 222);

        g.setColor(azul);
        g.setFont(new Font("Arial", Font.BOLD, 46));
        g.drawString("ALBARÁN Nº A-2026-0987", 90, 340);
        g.setColor(tinta);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        g.drawString("Fecha: " + LocalDate.now().format(DMY), 90, 392);
        g.drawString("Cliente: Bar La Plaza", 820, 392);

        int y = 500;
        int[] cols = {90, 760, 960, 1180};
        g.setColor(new Color(232, 238, 248));
        g.fillRect(80, y - 48, 1340, 70);
        g.setColor(tinta);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        String[] head = {"Descripción", "Cantidad", "Precio", "Importe"};
        for (int i = 0; i < head.length; i++) g.drawString(head[i], cols[i], y);
        String[][] lines = {
                {"Tomate pera cat. 1", "15 kg", "1,95", "29,25"},
                {"Limón primera", "5 kg", "1,90", "9,50"},
                {"Cebolla blanca saco 10 kg", "1 saco", "9,60", "9,60"},
                {"Patata agria", "20 kg", "0,79", "15,80"},
                {"Aceite oliva virgen extra 5 L", "2 garrafa", "33,90", "67,80"},
        };
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        for (String[] l : lines) {
            y += 82;
            for (int i = 0; i < l.length; i++) g.drawString(l[i], cols[i], y);
            g.setColor(new Color(220, 220, 215));
            g.drawLine(90, y + 26, 1410, y + 26);
            g.setColor(tinta);
        }
        y += 90;
        g.drawString("Base imponible: 131,95 €", 900, y);
        g.drawString("IVA (4% / 10%): 7,46 €", 900, y + 50);
        g.setFont(new Font("Arial", Font.BOLD, 38));
        g.drawString("TOTAL: 139,41 €", 900, y + 115);
        g.setFont(new Font("Arial", Font.ITALIC, 28));
        g.drawString("Recibí conforme:", 90, y + 260);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(40, 60, 120));
        g.drawArc(120, y + 290, 260, 60, 10, 160);
        g.drawLine(300, y + 300, 420, y + 340);
        g.dispose();

        BufferedImage photo = new BufferedImage(1600, 2040, BufferedImage.TYPE_INT_RGB);
        Graphics2D p = photo.createGraphics();
        p.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        p.setPaint(new GradientPaint(0, 0, new Color(132, 104, 78), 1600, 2040, new Color(92, 70, 52)));
        p.fillRect(0, 0, photo.getWidth(), photo.getHeight());
        AffineTransform t = new AffineTransform();
        t.translate(50, 60);
        t.rotate(Math.toRadians(-1.4), w / 2.0, h / 2.0);
        p.setColor(new Color(0, 0, 0, 70));
        p.fill(t.createTransformedShape(new Rectangle(14, 18, w, h)));
        p.drawImage(paper, t, null);
        // Luz desigual: más clara arriba a la izquierda
        p.setPaint(new GradientPaint(0, 0, new Color(255, 255, 240, 40), 1600, 2040, new Color(0, 0, 0, 55)));
        p.fillRect(0, 0, photo.getWidth(), photo.getHeight());
        p.dispose();
        Random r = new Random(11);
        for (int i = 0; i < 500_000; i++) {
            int x = r.nextInt(photo.getWidth()), yy = r.nextInt(photo.getHeight());
            Color c = new Color(photo.getRGB(x, yy));
            int d = r.nextInt(25) - 12;
            photo.setRGB(x, yy, new Color(cl(c.getRed() + d), cl(c.getGreen() + d), cl(c.getBlue() + d)).getRGB());
        }
        ImageIO.write(photo, "jpg", new File(out));
    }

    static void tarifa(String out) throws Exception {
        try (var wb = new XSSFWorkbook(); var os = new FileOutputStream(out)) {
            var sheet = wb.createSheet("Tarifa octubre");
            XSSFFont bold = wb.createFont();
            bold.setBold(true);
            CellStyle st = wb.createCellStyle();
            st.setFont(bold);
            String hasta = LocalDate.now().plusDays(40).format(DMY);
            String[][] rows = {
                    {"HUERTA DEL VALLE S.L.", "", ""},
                    {"Tarifa de precios · octubre 2026 · válida hasta el " + hasta, "", ""},
                    {"", "", ""},
                    {"Artículo", "Unidad", "Precio (sin IVA)"},
                    {"HORTALIZAS", "", ""},
                    {"Tomate pera categoría 1", "kg", "1,39"},
                    {"Cebolla blanca saco 10 kg", "saco", "8,40"},
                    {"Patata agria", "kg", "0,62"},
                    {"Pimiento rojo", "kg", "1,89"},
                    {"Lechuga iceberg", "ud", "0,79"},
                    {"FRUTA", "", ""},
                    {"Limón primera", "kg", "1,59"},
            };
            for (int i = 0; i < rows.length; i++) {
                var row = sheet.createRow(i);
                for (int c = 0; c < 3; c++) {
                    if (rows[i][c].isEmpty()) continue;
                    var cell = row.createCell(c);
                    cell.setCellValue(rows[i][c]);
                    if (i == 0 || i == 3 || rows[i][1].isEmpty()) cell.setCellStyle(st);
                }
            }
            sheet.setColumnWidth(0, 40 * 256);
            sheet.setColumnWidth(1, 12 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            wb.write(os);
        }
    }

    static void factura(String out) throws Exception {
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(out));
        doc.open();
        com.lowagie.text.Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        com.lowagie.text.Font b = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        com.lowagie.text.Font n = FontFactory.getFont(FontFactory.HELVETICA, 11);
        doc.add(new Paragraph("BEBIDAS COSTA S.L.", h1));
        doc.add(new Paragraph("CIF B-87654321 · Carrer del Port 12 · 08039 Barcelona · ventas@bebidascosta.test", n));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("FACTURA Nº F-2026-4471 · Fecha: " + LocalDate.now().minusDays(1).format(DMY), b));
        doc.add(new Paragraph("Cliente: Bar La Plaza · Barcelona", n));
        doc.add(new Paragraph(" "));
        PdfPTable t = new PdfPTable(new float[]{5, 1.6f, 1.6f, 1.6f});
        t.setWidthPercentage(100);
        for (String s : new String[]{"Concepto", "Cantidad", "Precio", "Importe"}) {
            PdfPCell c = new PdfPCell(new Phrase(s, b));
            c.setGrayFill(0.9f);
            t.addCell(c);
        }
        String[][] lines = {
                {"Agua mineral 1,5 L pack 6", "6", "2,75", "16,50"},
                {"Refresco cola lata 24 ud", "2", "13,20", "26,40"},
                {"Café en grano 1 kg", "2", "16,50", "33,00"},
                {"Aceite de oliva virgen extra 5 L", "1", "29,90", "29,90"},
        };
        for (String[] l : lines) {
            for (int i = 0; i < l.length; i++) {
                PdfPCell c = new PdfPCell(new Phrase(l[i], n));
                if (i > 0) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
                t.addCell(c);
            }
        }
        doc.add(t);
        doc.add(new Paragraph(" "));
        Paragraph tot = new Paragraph("Base imponible: 105,80 €   IVA: 13,09 €   TOTAL: 118,89 €", b);
        tot.setAlignment(Element.ALIGN_RIGHT);
        doc.add(tot);
        doc.close();
    }

    static int cl(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
