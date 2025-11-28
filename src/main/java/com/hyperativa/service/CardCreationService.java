package com.hyperativa.service;

import com.hyperativa.exception.DuplicateCardException;
import com.hyperativa.model.Card;
import com.hyperativa.model.User;
import com.hyperativa.repository.CardRepository;
import com.hyperativa.service.contract.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CardCreationService {

    private final CardRepository cardRepository;
    private final EncryptionService encryptionService;

    public CardCreationService(CardRepository cardRepository, EncryptionService encryptionService) {
        this.cardRepository = cardRepository;
        this.encryptionService = encryptionService;
    }

    public Card createCard(String cardNumber, User user) {
        validateCardNumber(cardNumber);
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);

        if (cardRepository.findByUserAndCardNumberEncrypted(user, encryptedCardNumber).isPresent()) {
            throw new DuplicateCardException("This card is already registered for this user");
        }

        Card card = new Card();
        card.setCardNumberEncrypted(encryptedCardNumber);
        card.setUser(user);

        return cardRepository.save(card);
    }

    private void validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            throw new RuntimeException("Card number cannot be empty");
        }

        if (!cardNumber.matches("\\d{13,19}")) {
            throw new RuntimeException("Invalid card number format");
        }
    }
}
