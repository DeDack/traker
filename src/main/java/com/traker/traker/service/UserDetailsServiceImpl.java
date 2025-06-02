package com.traker.traker.service;

import com.traker.traker.entity.User;
import com.traker.traker.exception.NotFoundException;
import com.traker.traker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Сервис для загрузки пользовательских данных по phone для Spring Security.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException("phone", phone) {
                    @Override
                    public String getEntityClassName() {
                        return "Пользователь";
                    }
                });
        return user;
    }
}