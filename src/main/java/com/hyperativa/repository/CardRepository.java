package com.hyperativa.repository;

import com.hyperativa.model.Card;
import com.hyperativa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByCardId(String cardId);

    Optional<Card> findByUserAndCardNumberEncrypted(User user, String encryptedCardNumber);
}
