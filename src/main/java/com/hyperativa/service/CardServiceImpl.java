package com.hyperativa.service;

import com.hyperativa.dto.BatchImportResponse;
import com.hyperativa.dto.CardLookupResponse;
import com.hyperativa.dto.CardRequest;
import com.hyperativa.dto.CardResponse;
import com.hyperativa.exception.CardNotFoundException;
import com.hyperativa.exception.UserNotFoundException;
import com.hyperativa.model.Card;
import com.hyperativa.model.User;
import com.hyperativa.repository.CardRepository;
import com.hyperativa.service.contract.CardService;
import com.hyperativa.service.contract.EncryptionService;
import com.hyperativa.service.contract.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final EncryptionService encryptionService;
    private final UserService userService;
    private final BatchFileImportService batchFileImportService;
    private final CardCreationService cardCreationService;

    public CardServiceImpl(CardRepository cardRepository, EncryptionService encryptionService, UserService userService,
                          BatchFileImportService batchFileImportService, CardCreationService cardCreationService) {
        this.cardRepository = cardRepository;
        this.encryptionService = encryptionService;
        this.userService = userService;
        this.batchFileImportService = batchFileImportService;
        this.cardCreationService = cardCreationService;
    }

    @Override
    public CardResponse create(CardRequest cardRequest, Long userId) {
        log.info("Inserting single card for user: {}", userId);
        User user = fetchUser(userId);

        String cardNumber = cardRequest.getCardNumber().trim();
        Card savedCard = cardCreationService.createCard(cardNumber, user);

        log.info("Card inserted successfully with ID: {}", savedCard.getCardId());
        return new CardResponse(savedCard.getCardId(), "Card inserted successfully");
    }

    @Override
    public CardLookupResponse lookupCard(Long userId, String cardNumber) {
        log.info("Looking up card");

        String encryptedCardNumber = encryptionService.encrypt(cardNumber.trim());

        Card card = cardRepository.findByUserAndCardNumberEncrypted(fetchUser(userId), encryptedCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        log.info("Card found with ID: {}", card.getCardId());
        return new CardLookupResponse(card.getCardId(), true);
    }

    @Override
    public BatchImportResponse importCardsFromFile(MultipartFile file, Long userId) {
        User user = fetchUser(userId);
        return batchFileImportService.importCardsFromFile(file, user);
    }

    public User fetchUser(Long userId) {
        return userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
