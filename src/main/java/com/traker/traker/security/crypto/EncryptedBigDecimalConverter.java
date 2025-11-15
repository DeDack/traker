package com.traker.traker.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/**
 * Encrypts {@link BigDecimal} values as strings in the database so financial
 * amounts remain opaque outside the application.
 */
@Converter(autoApply = false)
public class EncryptedBigDecimalConverter implements AttributeConverter<BigDecimal, String> {

    private static volatile DataEncryptionService encryptionService;

    public EncryptedBigDecimalConverter() {
        if (encryptionService == null) {
            throw new IllegalStateException("DataEncryptionService is not initialized for EncryptedBigDecimalConverter");
        }
    }

    static void registerEncryptionService(DataEncryptionService service) {
        encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionService.encrypt(EncryptionContextHolder.requireKey(), attribute.toPlainString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String plain = encryptionService.decrypt(EncryptionContextHolder.requireKey(), dbData);
        return new BigDecimal(plain);
    }
}
