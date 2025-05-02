package com.traker.traker.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.function.ThrowingSupplier;

import java.util.function.Consumer;

public final class CommonUtils {

    private static final String loggerName = "CommonUtils";
    private static final Logger logger = LoggerFactory.getLogger(loggerName);

    public static <T> T ignoreException(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            logger.error(loggerName + ": ignoreException() -> " + e.getMessage());
            return null;
        }
    }

    public static void setIfNotBlank(String obj, Consumer<String> setter) {
        if (obj != null && !obj.isBlank()) {
            setter.accept(obj);
        }
    }

    public static <T> void setIfNotBlank(T obj, Consumer<T> setter) {
        if (obj != null) {
            setter.accept(obj);
        }
    }
}
