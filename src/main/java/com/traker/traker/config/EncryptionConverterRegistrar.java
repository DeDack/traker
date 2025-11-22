package com.traker.traker.config;

import com.traker.traker.security.crypto.DataEncryptionService;
import com.traker.traker.security.crypto.EncryptedBigDecimalConverter;
import com.traker.traker.security.crypto.EncryptedStringConverter;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Ensures that attribute converters obtain a reference to the Spring-managed
 * {@link DataEncryptionService} once the application context is ready. Without
 * this explicit registration Hibernate might attempt to instantiate the
 * converters before Spring finishes bootstrapping, leading to missing-bean
 * failures.
 */
@Configuration
public class EncryptionConverterRegistrar {

    private final DataEncryptionService encryptionService;

    public EncryptionConverterRegistrar(DataEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @PostConstruct
    void registerConverters() {
        EncryptedStringConverter.registerEncryptionService(encryptionService);
        EncryptedBigDecimalConverter.registerEncryptionService(encryptionService);
    }
}
