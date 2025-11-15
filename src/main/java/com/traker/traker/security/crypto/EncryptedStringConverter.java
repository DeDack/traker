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

    public static void registerEncryptionService(DataEncryptionService service) {
        encryptionService = service;
    }

    private static DataEncryptionService requireService() {
        DataEncryptionService service = encryptionService;
        if (service == null) {
            throw new IllegalStateException("DataEncryptionService is not initialized for EncryptedStringConverter");
        }
        return service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return requireService().encrypt(EncryptionContextHolder.requireKey(), attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return requireService().decrypt(EncryptionContextHolder.requireKey(), dbData);
    }
}
