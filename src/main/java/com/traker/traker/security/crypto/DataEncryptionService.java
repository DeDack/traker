package com.traker.traker.security.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Provides AES/GCM helpers for encrypting arbitrary payloads with a
 * user-specific key. The encrypted value is stored as Base64 (IV + ciphertext).
 */
@Service
public class DataEncryptionService {

    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();
    private int keySizeBytes = 32;

    @PostConstruct
    void validateSecurityProvider() {
        // Trigger early failure if the runtime does not support the required cipher.
        try {
            Cipher.getInstance(TRANSFORMATION);
            int maxKeyLength = Cipher.getMaxAllowedKeyLength(AES);
            this.keySizeBytes = selectKeySize(maxKeyLength);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES/GCM is not available in the current JVM", e);
        }
    }

    public byte[] generateKey() {
        byte[] key = new byte[keySizeBytes];
        secureRandom.nextBytes(key);
        return key;
    }

    public byte[] decodeKey(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException("Master key value must be provided");
        }

        byte[] decoded = tryBase64Decode(encoded.trim());
        return normalizeKeyLength(decoded);
    }

    private static int selectKeySize(int maxKeyLength) {
        if (maxKeyLength >= 256) {
            return 32;
        }
        if (maxKeyLength >= 192) {
            return 24;
        }
        if (maxKeyLength >= 128) {
            return 16;
        }
        throw new IllegalStateException("AES key sizes below 128 bits are not supported");
    }

    private static byte[] tryBase64Decode(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static boolean isValidKeyLength(int length) {
        return length == 16 || length == 24 || length == 32;
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available", e);
        }
    }

    private byte[] normalizeKeyLength(byte[] candidate) {
        byte[] normalized = candidate;
        if (!isValidKeyLength(normalized.length)) {
            normalized = sha256(normalized);
        }

        if (normalized.length > keySizeBytes) {
            return Arrays.copyOf(normalized, keySizeBytes);
        }

        return normalized;
    }

    public String encrypt(byte[] key, String plainText) {
        if (plainText == null) {
            return null;
        }
        return encryptBytes(key, plainText.getBytes(StandardCharsets.UTF_8));
    }

    public String encryptBytes(byte[] key, byte[] plainBytes) {
        if (plainBytes == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, AES), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherBytes = cipher.doFinal(plainBytes);

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt secure data", e);
        }
    }

    public String decrypt(byte[] key, String cipherText) {
        byte[] bytes = decryptToBytes(key, cipherText);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] decryptToBytes(byte[] key, String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, AES), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(cipherBytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt secure data", e);
        }
    }
}
