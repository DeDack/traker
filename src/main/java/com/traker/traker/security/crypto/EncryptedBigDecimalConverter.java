package com.traker.traker.security.crypto;

import com.traker.traker.config.ApplicationContextProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/**
 * Encrypts {@link BigDecimal} values as strings in the database so financial
 * amounts remain opaque outside the application.
 */
@Converter(autoApply = false)
public class EncryptedBigDecimalConverter implements AttributeConverter<BigDecimal, String> {

    private final DataEncryptionService encryptionService;

    public EncryptedBigDecimalConverter() {
        this.encryptionService = ApplicationContextProvider.getBean(DataEncryptionService.class);
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
