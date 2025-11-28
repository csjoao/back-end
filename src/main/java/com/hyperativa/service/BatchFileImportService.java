package com.hyperativa.service;

import com.hyperativa.dto.BatchImportResponse;
import com.hyperativa.exception.DuplicateCardException;
import com.hyperativa.model.Card;
import com.hyperativa.model.User;
import com.hyperativa.repository.CardRepository;
import com.hyperativa.service.contract.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BatchFileImportService {

    private final CardRepository cardRepository;
    private final EncryptionService encryptionService;
    private final CardCreationService cardCreationService;

    public BatchFileImportService(CardRepository cardRepository, EncryptionService encryptionService,
                                  CardCreationService cardCreationService) {
        this.cardRepository = cardRepository;
        this.encryptionService = encryptionService;
        this.cardCreationService = cardCreationService;
    }

    public BatchImportResponse importCardsFromFile(MultipartFile file, User user) {
        log.info("Starting batch import for user: {}", user.getId());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<String> allLines = readAllLines(reader);

            if (allLines.isEmpty()) {
                return new BatchImportResponse(0, 0, 0, "File is empty");
            }

            BatchFileContent content = parseFileContent(allLines);
            return processBatch(content, user);

        } catch (Exception e) {
            log.error("Error reading file for batch import", e);
            return new BatchImportResponse(0, 0, 1, "Error reading file: " + e.getMessage());
        }
    }

    private List<String> readAllLines(BufferedReader reader) throws Exception {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private BatchFileContent parseFileContent(List<String> allLines) {
        String headerLine = allLines.get(0);
        int expectedRecords;

        try {
            expectedRecords = parseHeader(headerLine);
            log.info("Header parsed successfully. Expected records: {}", expectedRecords);
        } catch (Exception e) {
            log.error("Error parsing header line: {}", headerLine, e);
            throw new IllegalArgumentException("Invalid file header: " + e.getMessage());
        }

        // Validate that the number of card records matches the expected quantity
        int actualCardCount = countCardRecords(allLines);
        if (actualCardCount != expectedRecords) {
            String validationError = String.format(
                    "Record count mismatch. Header declares %d records, but found %d card lines",
                    expectedRecords, actualCardCount);
            log.error(validationError);
            throw new IllegalArgumentException(validationError);
        }

        return new BatchFileContent(expectedRecords, allLines);
    }

    private BatchImportResponse processBatch(BatchFileContent content, User user) {
        int successCount = 0;
        int errorCount = 0;
        int recordsProcessed = 0;
        String errorMessage = null;

        for (int i = 1; i < content.allLines.size(); i++) {
            String line = content.allLines.get(i);
            int lineNumber = i + 1;

            if (line.trim().isEmpty()) {
                continue;
            }

            if (isTrailer(line)) {
                try {
                    validateTrailer(line, recordsProcessed);
                    log.info("Trailer validated successfully");
                } catch (Exception e) {
                    log.warn("Trailer validation failed: {}", e.getMessage());
                    errorMessage = e.getMessage();
                }
                continue;
            }

            try {
                if (processCard(line, user)) {
                    successCount++;
                    recordsProcessed++;
                } else {
                    errorCount++;
                }
            } catch (Exception e) {
                log.error("Error processing card at line {}: {}", lineNumber, e.getMessage());
                errorCount++;
            }
        }

        return buildResponse(content.expectedRecords, recordsProcessed, successCount, errorCount, errorMessage);
    }

    private boolean processCard(String line, User user) {
        String cardNumber = extractCardNumber(line);

        if (cardNumber == null || cardNumber.isEmpty()) {
            log.warn("Unable to extract card number from line: {}", line);
            return false;
        }

        try {
            cardCreationService.createCard(cardNumber, user);
            return true;
        } catch (DuplicateCardException e) {
            log.warn("Skipping duplicate card");
            return false;
        }
    }

    private BatchImportResponse buildResponse(int expectedRecords, int recordsProcessed, int successCount,
                                              int errorCount, String errorMessage) {
        String message = String.format("Batch import completed. Expected: %d, Processed: %d, Success: %d, Errors: %d",
                expectedRecords, recordsProcessed, successCount, errorCount);

        if (errorMessage != null) {
            message += ". Warning: " + errorMessage;
        }

        log.info(message);
        return new BatchImportResponse(expectedRecords, successCount, errorCount, message);
    }

    private int parseHeader(String headerLine) {
        if (headerLine.length() < 51) {
            throw new IllegalArgumentException("Header line must be at least 51 characters long");
        }

        String nome = headerLine.substring(0, 29).trim();
        String data = headerLine.substring(29, 37).trim();
        String lote = headerLine.substring(37, 45).trim();
        String qtyStr = headerLine.substring(45, 51).trim();

        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Batch name (position 1-29) is empty");
        }
        if (data.isEmpty() || !data.matches("\\d{8}")) {
            throw new IllegalArgumentException("Invalid date format (position 30-37). Expected YYYYMMDD");
        }
        if (lote.isEmpty()) {
            throw new IllegalArgumentException("Batch ID (position 38-45) is empty");
        }

        try {
            int quantity = Integer.parseInt(qtyStr);
            if (quantity < 0) {
                throw new IllegalArgumentException("Record quantity cannot be negative");
            }
            return quantity;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid record quantity format (position 46-51). Expected numeric value");
        }
    }

    private int countCardRecords(List<String> allLines) {
        int cardCount = 0;

        // Start from line 1 (skip header at position 0)
        for (int i = 1; i < allLines.size(); i++) {
            String line = allLines.get(i);

            if (line.trim().isEmpty()) {
                continue;
            }

            // Skip trailer lines
            if (isTrailer(line)) {
                continue;
            }

            // Count lines that start with 'C' (card records)
            if (line.length() > 0 && line.substring(0, 1).trim().equalsIgnoreCase("C")) {
                cardCount++;
            }
        }

        return cardCount;
    }

    private boolean isTrailer(String line) {
        return line.length() >= 8 && line.substring(0, 4).trim().equalsIgnoreCase("LOTE");
    }

    private void validateTrailer(String trailerLine, int recordsProcessed) {
        if (trailerLine.length() < 14) {
            throw new IllegalArgumentException("Trailer line must be at least 14 characters long");
        }

        String lote = trailerLine.substring(0, 8).trim();
        String qtyStr = trailerLine.substring(8, 14).trim();

        if (!lote.equalsIgnoreCase("LOTE")) {
            throw new IllegalArgumentException("Invalid trailer format. Expected LOTE at position 1-8");
        }

        try {
            int trailerQty = Integer.parseInt(qtyStr);
            if (trailerQty != recordsProcessed) {
                throw new IllegalArgumentException(
                        String.format("Trailer record count (%d) does not match processed records (%d)",
                                trailerQty, recordsProcessed));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid record quantity in trailer (position 9-14)");
        }
    }

    private String extractCardNumber(String line) {
        if (line.length() < 8) {
            return null;
        }

        String type = line.substring(0, 1).trim();
        if (!type.equalsIgnoreCase("C")) {
            return null;
        }

        int endIndex = Math.min(26, line.length());
        String cardNumber = line.substring(7, endIndex).trim();

        return cardNumber.isEmpty() ? null : cardNumber;
    }

    private static class BatchFileContent {
        int expectedRecords;
        List<String> allLines;

        BatchFileContent(int expectedRecords, List<String> allLines) {
            this.expectedRecords = expectedRecords;
            this.allLines = allLines;
        }
    }
}
