package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.dto.RawTransactionData;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FileParsingService {


    public List<RawTransactionData> parseCSV(MultipartFile file, String dateFormat, String currency) throws IOException {
        List<RawTransactionData> transactions = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            int rowNumber = 0;

            reader.readNext();
            rowNumber++;
            
            while ((line = reader.readNext()) != null) {
                try {
                    RawTransactionData transaction = parseCSVRow(line, formatter, currency, rowNumber);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing CSV row {}: {}", rowNumber, e.getMessage());
                }
                rowNumber++;
            }
        } catch (CsvValidationException e) {
            throw new IOException("Invalid CSV format", e);
        }
        
        log.info("Parsed {} transactions from CSV file", transactions.size());
        return transactions;
    }


    public List<RawTransactionData> parseExcel(MultipartFile file, String dateFormat, String currency) throws IOException {
        List<RawTransactionData> transactions = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    try {
                        RawTransactionData transaction = parseExcelRow(row, formatter, currency, rowIndex + 1);
                        if (transaction != null) {
                            transactions.add(transaction);
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing Excel row {}: {}", rowIndex + 1, e.getMessage());
                    }
                }
            }
        }
        
        log.info("Parsed {} transactions from Excel file", transactions.size());
        return transactions;
    }


    public List<RawTransactionData> parseFile(MultipartFile file, String dateFormat, String currency) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IOException("File name is null");
        }
        
        if (fileName.toLowerCase().endsWith(".csv")) {
            return parseCSV(file, dateFormat, currency);
        } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls")) {
            return parseExcel(file, dateFormat, currency);
        } else {
            throw new IOException("Unsupported file format. Please use CSV or Excel files.");
        }
    }

    private RawTransactionData parseCSVRow(String[] line, DateTimeFormatter formatter, String currency, int rowNumber) {
        if (line.length < 3) {
            log.warn("Row {} has insufficient columns: {}", rowNumber, line.length);
            return null;
        }
        
        try {
            LocalDate date = parseDate(line[0], formatter);
            BigDecimal amount = parseAmount(line[1]);
            String type = line.length > 2 ? line[2].trim() : "purchase";
            String description = line.length > 3 ? line[3].trim() : "";
            String category = line.length > 4 ? line[4].trim() : "";
            
            return RawTransactionData.builder()
                    .date(date)
                    .amount(amount)
                    .type(type)
                    .description(description)
                    .category(category)
                    .currency(currency)
                    .rowNumber(rowNumber)
                    .build();
                    
        } catch (Exception e) {
            log.warn("Error parsing CSV row {}: {}", rowNumber, e.getMessage());
            return null;
        }
    }

    private RawTransactionData parseExcelRow(Row row, DateTimeFormatter formatter, String currency, int rowNumber) {
        try {
            LocalDate date = parseExcelDate(row.getCell(0), formatter);
            BigDecimal amount = parseExcelAmount(row.getCell(1));
            String type = getCellValue(row.getCell(2), "purchase");
            String description = getCellValue(row.getCell(3), "");
            String category = getCellValue(row.getCell(4), "");
            
            return RawTransactionData.builder()
                    .date(date)
                    .amount(amount)
                    .type(type)
                    .description(description)
                    .category(category)
                    .currency(currency)
                    .rowNumber(rowNumber)
                    .build();
                    
        } catch (Exception e) {
            log.warn("Error parsing Excel row {}: {}", rowNumber, e.getMessage());
            return null;
        }
    }

    private LocalDate parseDate(String dateStr, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(dateStr.trim(), formatter);
        } catch (DateTimeParseException e) {

            String[] fallbackFormats = {"MM/dd/yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy"};
            for (String format : fallbackFormats) {
                try {
                    return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(format));
                } catch (DateTimeParseException ignored) {

                }
            }
            throw new IllegalArgumentException("Unable to parse date: " + dateStr);
        }
    }

    private LocalDate parseExcelDate(Cell cell, DateTimeFormatter formatter) {
        if (cell == null) {
            throw new IllegalArgumentException("Date cell is null");
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return parseDate(cell.getStringCellValue(), formatter);
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate();
                } else {
                    return LocalDate.of(1900, 1, 1).plusDays((long) cell.getNumericCellValue() - 2);
                }
            default:
                throw new IllegalArgumentException("Invalid date cell type: " + cell.getCellType());
        }
    }

    private BigDecimal parseAmount(String amountStr) {
        try {
            String cleanAmount = amountStr.trim()
                    .replaceAll("[$,€£¥]", "")
                    .replaceAll(",", "");
            
            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse amount: " + amountStr);
        }
    }

    private BigDecimal parseExcelAmount(Cell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("Amount cell is null");
        }

        return switch (cell.getCellType()) {
            case STRING -> parseAmount(cell.getStringCellValue());
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            default -> throw new IllegalArgumentException("Invalid amount cell type: " + cell.getCellType());
        };
    }

    private String getCellValue(Cell cell, String defaultValue) {
        if (cell == null) {
            return defaultValue;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> defaultValue;
        };
    }
} 