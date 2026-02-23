package com.addverb.outbound_service.service;

import com.addverb.outbound_service.entity.Order;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class OrderExcelExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] exportOrdersToExcel(List<Order> orders) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Orders");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {
                    "Order Number",
                    "Customer Name",
                    "Address",
                    "SKU Code",
                    "MRP",
                    "Requested Quantity",
                    "Allocated Quantity",
                    "Status",
                    "Created At",
                    "Updated At"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < orders.size(); i++) {
                Order order = orders.get(i);
                Row row = sheet.createRow(i + 1);

                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(order.getCustomerName());
                row.createCell(2).setCellValue(order.getAddress());
                row.createCell(3).setCellValue(order.getSkuCode());
                row.createCell(4).setCellValue(order.getMrp() != null ? order.getMrp() : 0.0);
                row.createCell(5).setCellValue(order.getRequestedQty() != null ? order.getRequestedQty() : 0);
                row.createCell(6).setCellValue(order.getAllocatedQty() != null ? order.getAllocatedQty() : 0);
                row.createCell(7).setCellValue(order.getStatus() != null ? order.getStatus().name() : "");
                row.createCell(8).setCellValue(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_TIME_FORMATTER) : "");
                row.createCell(9).setCellValue(order.getUpdatedAt() != null ? order.getUpdatedAt().format(DATE_TIME_FORMATTER) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate order export file", ex);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(font);
        return headerStyle;
    }
}




