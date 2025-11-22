package com.traker.traker.security.crypto;

import java.util.Arrays;

/**
 * Stores the current user's decrypted data key in a {@link ThreadLocal} so any
 * persistence converter can transparently encrypt/decrypt values during the
 * lifespan of a web request.
 */
public final class EncryptionContextHolder {

    private static final ThreadLocal<byte[]> KEY_HOLDER = new ThreadLocal<>();

    private EncryptionContextHolder() {
    }

    public static void setKey(byte[] key) {
        if (key == null) {
            KEY_HOLDER.remove();
            return;
        }
        KEY_HOLDER.set(Arrays.copyOf(key, key.length));
    }

    public static byte[] requireKey() {
        byte[] key = KEY_HOLDER.get();
        if (key == null) {
            throw new IllegalStateException("Encryption key is not available for the current context");
        }
        return key;
    }

    public static void clear() {
        KEY_HOLDER.remove();
    }
}
