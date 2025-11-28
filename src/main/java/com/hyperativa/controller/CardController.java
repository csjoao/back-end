package com.hyperativa.controller;

import com.hyperativa.dto.ApiResponse;
import com.hyperativa.dto.BatchImportResponse;
import com.hyperativa.dto.CardLookupResponse;
import com.hyperativa.dto.CardRequest;
import com.hyperativa.dto.CardResponse;
import com.hyperativa.service.contract.CardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Slf4j
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CardResponse>> insertCard(@Valid @RequestBody CardRequest cardRequest, HttpServletRequest httpRequest) {
        log.info("Inserting single card");
        Long userId = (Long) httpRequest.getAttribute("userId");
        CardResponse response = cardService.create(cardRequest, userId);
        log.info("Card inserted successfully with ID: {}", response.getCardId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Card inserted successfully", HttpStatus.CREATED.value()));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<BatchImportResponse>> importCards(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        log.info("Starting batch import");
        Long userId = (Long) httpRequest.getAttribute("userId");

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File cannot be empty", HttpStatus.BAD_REQUEST.value()));
        }

        BatchImportResponse response = cardService.importCardsFromFile(file, userId);
        log.info("Batch import completed: {} success, {} errors", response.getSuccessCount(), response.getErrorCount());
        return ResponseEntity.ok(ApiResponse.success(response, "Batch import completed successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CardLookupResponse>> lookupCard(
            @RequestParam("cardNumber") @NotBlank(message = "Card number is required") String cardNumber, HttpServletRequest httpRequest) {
        log.info("Looking up card");
        Long userId = (Long) httpRequest.getAttribute("userId");
        CardLookupResponse response = cardService.lookupCard(userId, cardNumber);
        log.info("Card lookup completed - Found: {}", response.isFound());
        return ResponseEntity.ok(ApiResponse.success(response, "Card lookup completed"));
    }
}
