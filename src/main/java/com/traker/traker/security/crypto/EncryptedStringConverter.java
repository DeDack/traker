package com.traker.traker.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter that transparently encrypts/decrypts string values using the
 * per-request key stored in {@link EncryptionContextHolder}.
 */
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static volatile DataEncryptionService encryptionService;

    public EncryptedStringConverter() {
        if (encryptionService == null) {
            throw new IllegalStateException("DataEncryptionService is not initialized for EncryptedStringConverter");
        }
    }

    static void registerEncryptionService(DataEncryptionService service) {
        encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionService.encrypt(EncryptionContextHolder.requireKey(), attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptionService.decrypt(EncryptionContextHolder.requireKey(), dbData);
    }
}
