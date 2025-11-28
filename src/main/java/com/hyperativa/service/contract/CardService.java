package com.hyperativa.service.contract;

import com.hyperativa.dto.BatchImportResponse;
import com.hyperativa.dto.CardLookupResponse;
import com.hyperativa.dto.CardRequest;
import com.hyperativa.dto.CardResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CardService {

    CardResponse create(CardRequest cardRequest, Long userId);

    CardLookupResponse lookupCard(Long userId, String cardNumber);

    BatchImportResponse importCardsFromFile(MultipartFile file, Long userId);
}
