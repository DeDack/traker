package com.traker.traker.security;

import com.traker.traker.entity.User;
import com.traker.traker.security.crypto.EncryptionContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ensures the decrypted user key is available to JPA converters during each
 * request lifecycle.
 */
@Component
public class EncryptionContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                byte[] key = user.getDecryptedDataKey();
                if (key != null) {
                    EncryptionContextHolder.setKey(key);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            EncryptionContextHolder.clear();
        }
    }
}
