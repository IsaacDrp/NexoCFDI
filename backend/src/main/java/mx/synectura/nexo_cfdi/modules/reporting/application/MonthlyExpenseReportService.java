package mx.synectura.nexo_cfdi.modules.reporting.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence.IngestedEmailEntity;
import mx.synectura.nexo_cfdi.modules.ingestion.infrastructure.persistence.IngestedEmailJpaRepository;
import mx.synectura.nexo_cfdi.shared.domain.user.persistence.UserEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlyExpenseReportService {

    private final IngestedEmailJpaRepository repository;

    public byte[] generateReport(UserEntity user, int year, int month) {
        LocalDateTime from = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime to = from.plusMonths(1);

        List<IngestedEmailEntity> cfdis = repository.findParsedCfdisByUserAndMonth(user.getId(), from, to);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Gastos " + month + "-" + year);

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("$#,##0.00"));

            CellStyle rowStyleLight = workbook.createCellStyle();
            rowStyleLight.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            rowStyleLight.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle currencyStyleLight = workbook.createCellStyle();
            currencyStyleLight.setDataFormat(format.getFormat("$#,##0.00"));
            currencyStyleLight.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            currencyStyleLight.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Cabecera del Usuario
            Row userHeaderRow = sheet.createRow(0);
            userHeaderRow.createCell(0).setCellValue("Razón Social:");
            userHeaderRow.createCell(1).setCellValue(user.getFirstName() + " " + (user.getPaternalSurname() != null ? user.getPaternalSurname() : ""));
            
            Row rfcRow = sheet.createRow(1);
            rfcRow.createCell(0).setCellValue("RFC:");
            rfcRow.createCell(1).setCellValue(user.getRfc() != null ? user.getRfc() : "N/A");

            // Tabla de Gastos - Encabezados
            String[] columns = {"Razón Social Emisor", "RFC Emisor", "Fecha", "Subtotal", "IVA", "Total", "UUID"};
            Row tableHeaderRow = sheet.createRow(3);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = tableHeaderRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            BigDecimal totalSubtotal = BigDecimal.ZERO;
            BigDecimal totalIva = BigDecimal.ZERO;
            BigDecimal totalGlobal = BigDecimal.ZERO;

            for (IngestedEmailEntity cfdi : cfdis) {
                Row row = sheet.createRow(rowIdx++);
                boolean isLight = rowIdx % 2 == 0;
                CellStyle currentTextStyle = isLight ? rowStyleLight : null;
                CellStyle currentCurrencyStyle = isLight ? currencyStyleLight : currencyStyle;

                Cell c0 = row.createCell(0);
                c0.setCellValue(cfdi.getCfdiNombreEmisor() != null ? cfdi.getCfdiNombreEmisor() : "");
                if (currentTextStyle != null) c0.setCellStyle(currentTextStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(cfdi.getCfdiRfcEmisor() != null ? cfdi.getCfdiRfcEmisor() : "");
                if (currentTextStyle != null) c1.setCellStyle(currentTextStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(cfdi.getCfdiFecha() != null ? cfdi.getCfdiFecha().toString() : "");
                if (currentTextStyle != null) c2.setCellStyle(currentTextStyle);

                Cell subtotalCell = row.createCell(3);
                BigDecimal subtotal = cfdi.getCfdiSubtotal() != null ? cfdi.getCfdiSubtotal() : BigDecimal.ZERO;
                subtotalCell.setCellValue(subtotal.doubleValue());
                subtotalCell.setCellStyle(currentCurrencyStyle);
                totalSubtotal = totalSubtotal.add(subtotal);

                Cell ivaCell = row.createCell(4);
                BigDecimal iva = cfdi.getCfdiIva() != null ? cfdi.getCfdiIva() : BigDecimal.ZERO;
                ivaCell.setCellValue(iva.doubleValue());
                ivaCell.setCellStyle(currentCurrencyStyle);
                totalIva = totalIva.add(iva);

                Cell totalCell = row.createCell(5);
                BigDecimal total = cfdi.getCfdiTotal() != null ? cfdi.getCfdiTotal() : BigDecimal.ZERO;
                totalCell.setCellValue(total.doubleValue());
                totalCell.setCellStyle(currentCurrencyStyle);
                totalGlobal = totalGlobal.add(total);

                Cell c6 = row.createCell(6);
                c6.setCellValue(cfdi.getCfdiUuid() != null ? cfdi.getCfdiUuid() : "");
                if (currentTextStyle != null) c6.setCellStyle(currentTextStyle);
            }

            // Resumen Final
            rowIdx++;
            Row summaryHeaderRow = sheet.createRow(rowIdx++);
            summaryHeaderRow.createCell(2).setCellValue("RESUMEN:");
            summaryHeaderRow.getCell(2).setCellStyle(headerStyle);

            Row subtotalRow = sheet.createRow(rowIdx++);
            subtotalRow.createCell(2).setCellValue("Total de Subtotal:");
            Cell totalSubCell = subtotalRow.createCell(3);
            totalSubCell.setCellValue(totalSubtotal.doubleValue());
            totalSubCell.setCellStyle(currencyStyle);

            Row sumIvaRow = sheet.createRow(rowIdx++);
            sumIvaRow.createCell(2).setCellValue("Total de IVA:");
            Cell sumIvaCell = sumIvaRow.createCell(3);
            sumIvaCell.setCellValue(totalIva.doubleValue());
            sumIvaCell.setCellStyle(currencyStyle);

            Row sumTotalRow = sheet.createRow(rowIdx++);
            sumTotalRow.createCell(2).setCellValue("Total de Total:");
            Cell sumTotalCell = sumTotalRow.createCell(3);
            sumTotalCell.setCellValue(totalGlobal.doubleValue());
            sumTotalCell.setCellStyle(currencyStyle);

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating excel report", e);
            throw new RuntimeException("Error generating excel report", e);
        }
    }
}
