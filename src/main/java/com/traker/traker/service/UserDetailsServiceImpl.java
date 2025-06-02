package com.traker.traker.service;

import com.traker.traker.entity.User;
import com.traker.traker.exception.NotFoundException;
import com.traker.traker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Сервис для загрузки пользовательских данных по username для Spring Security.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("username", username) {
                    @Override
                    public String getEntityClassName() {
                        return "Пользователь";
                    }
                });
        return user;
    }
}
