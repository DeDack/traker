package com.traker.traker.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility component that exposes the Spring {@link ApplicationContext} so that
 * JPA converters can lazily obtain beans without traditional dependency
 * injection (which is not supported for converters instantiated by Hibernate).
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> beanType) {
        if (context == null) {
            throw new IllegalStateException("Spring application context is not initialized yet");
        }
        return context.getBean(beanType);
    }
}
