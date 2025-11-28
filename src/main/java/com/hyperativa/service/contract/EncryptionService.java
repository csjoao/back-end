package com.hyperativa.service.contract;

public interface EncryptionService {

    String encrypt(String plainText);

    String decrypt(String encryptedText);
}
