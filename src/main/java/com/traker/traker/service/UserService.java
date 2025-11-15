package com.traker.traker.service;

import com.traker.traker.api.DefaultService;
import com.traker.traker.dto.AuthRequestDto;
import com.traker.traker.dto.UserDto;
import com.traker.traker.dto.CreateUserDto;
import com.traker.traker.dto.UpdateUserDto;
import com.traker.traker.entity.Role;
import com.traker.traker.entity.User;
import com.traker.traker.exception.NotFoundException;
import com.traker.traker.exception.UnauthorizedException;
import com.traker.traker.exception.UserAlreadyExistsException;
import com.traker.traker.mapper.RoleMapper;
import com.traker.traker.mapper.UserMapper;
import com.traker.traker.repository.RoleRepository;
import com.traker.traker.repository.UserRepository;
import com.traker.traker.security.UserEncryptionService;
import com.traker.traker.utils.CommonUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Сервис для управления пользователями.
 */
@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
@Transactional
public class UserService extends DefaultService<Long, User, UserDto> {

    UserRepository userRepository;
    UserMapper userMapper;
    RoleRepository roleRepository;
    RoleMapper roleMapper;
    PasswordEncoder passwordEncoder;
    AuthService authService;
    UserEncryptionService userEncryptionService;

    /**
     * Конструктор для инициализации сервиса и зависимостей.
     *
     * @param userRepository   Репозиторий пользователей
     * @param userMapper       Маппер для пользователей
     * @param roleRepository   Репозиторий ролей
     * @param roleMapper       Маппер для ролей
     * @param passwordEncoder  Кодировщик паролей
     * @param authService      Сервис аутентификации
     */
    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       RoleRepository roleRepository,
                       RoleMapper roleMapper,
                       PasswordEncoder passwordEncoder,
                       AuthService authService,
                       UserEncryptionService userEncryptionService) {
        super(userRepository, userMapper, id -> new NotFoundException("User", id) {
            @Override
            public String getEntityClassName() {
                return "Пользователь";
            }
        });
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.userEncryptionService = userEncryptionService;
    }

    /**
     * Создает нового пользователя и выполняет автоматический логин.
     *
     * @param createUserDto DTO с данными для создания пользователя
     * @param response      Ответ для установки cookies с токенами
     * @return DTO созданного пользователя
     */
    @Transactional
    public UserDto createUser(CreateUserDto createUserDto, HttpServletResponse response) {
        String normalizedUsername = createUserDto.getUsername();
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new UserAlreadyExistsException("Пользователь с именем " + normalizedUsername + " уже существует");
        }

        User user = userMapper.toEntity(createUserDto);
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(createUserDto.getPassword()));
        userEncryptionService.assignFreshKey(user);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new NotFoundException("Role", "USER") {
                    @Override
                    public String getEntityClassName() {
                        return "Роль";
                    }
                });
        user.setRoles(Collections.singleton(userRole));

        User savedUser = userRepository.save(user);

        // Автоматический логин
        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername(createUserDto.getUsername());
        authRequest.setPassword(createUserDto.getPassword());
        authService.login(authRequest, response);

        return userMapper.toDto(savedUser);
    }

    /**
     * Обновляет данные текущего пользователя.
     *
     * @param id            ID пользователя
     * @param updateUserDto DTO с данными для обновления
     * @return DTO обновленного пользователя
     */
    @Transactional
    public UserDto updateUser(Long id, UpdateUserDto updateUserDto) {
        User currentUser = getCurrentUser();
        if (!currentUser.getId().equals(id)) {
            throw new UnauthorizedException("Вы можете обновлять только свои данные");
        }

        User user = findByIdInternal(id);
        CommonUtils.setIfNotBlank(updateUserDto.getName(), user::setName);
        CommonUtils.setIfNotBlank(updateUserDto.getPassword(), password ->
                user.setPassword(passwordEncoder.encode(password)));
        CommonUtils.setIfNotBlank(updateUserDto.getUsername(), username -> {
            String normalizedUsername = username;
            if (userRepository.existsByUsername(normalizedUsername) && !normalizedUsername.equals(user.getUsername())) {
                throw new UserAlreadyExistsException("Имя пользователя " + normalizedUsername + " уже занято");
            }
            user.setUsername(normalizedUsername);
        });
        user.setUpdatedAt(LocalDateTime.now());

        return userMapper.toDto(userRepository.save(user));
    }

    /**
     * Удаляет текущего пользователя.
     *
     * @param id ID пользователя
     */
    @Transactional
    public void deleteUser(Long id) {
        User currentUser = getCurrentUser();
        if (!currentUser.getId().equals(id)) {
            throw new UnauthorizedException("Вы можете удалять только свой аккаунт");
        }
        userRepository.deleteById(id);
    }

    /**
     * Получает данные текущего аутентифицированного пользователя.
     *
     * @return DTO текущего пользователя
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUserDto() {
        User user = getCurrentUser();
        return userMapper.toDto(user);
    }

    /**
     * Возвращает текущего аутентифицированного пользователя.
     *
     * @return Текущий пользователь
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User", username) {
                    @Override
                    public String getEntityClassName() {
                        return "Пользователь";
                    }
                });
    }

    /**
     * Нормализует имя пользователя.
     *
     * @param username Имя пользователя
     * @return Нормализованное имя
     */
    private String normalizeUsername(String username) {
        return username;
    }

    @Override
    protected User updateInternal(User user, UserDto userDto) {
        // Не используется, так как обновление через UpdateUserDto
        throw new UnsupportedOperationException("Обновление через UserDto не поддерживается");
    }
}