package com.traker.traker.service;

import com.traker.traker.api.DefaultService;
import com.traker.traker.dto.StatusDto;
import com.traker.traker.entity.Status;
import com.traker.traker.entity.User;
import com.traker.traker.exception.StatusNotFoundException;
import com.traker.traker.mapper.StatusMapper;
import com.traker.traker.repository.StatusRepository;
import com.traker.traker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class StatusService extends DefaultService<Long, Status, StatusDto> {

    private final StatusMapper statusMapper;
    private final StatusRepository statusRepository;
    private final UserRepository userRepository;

    public StatusService(StatusMapper statusMapper, StatusRepository statusRepository, UserRepository userRepository) {
        super(statusRepository, statusMapper, StatusNotFoundException::new);
        this.statusMapper = statusMapper;
        this.statusRepository = statusRepository;
        this.userRepository = userRepository;
    }

    public StatusDto createStatus(StatusDto statusDto) {
        User currentUser = getCurrentUser();
        Status status = statusMapper.toEntity(statusDto);
        status.setUser(currentUser);
        int nextOrder = statusDto.getOrder() != null
                ? Math.max(0, statusDto.getOrder())
                : statusRepository.findTopByUserOrderByOrderIndexDesc(currentUser)
                .map(existing -> existing.getOrderIndex() + 1)
                .orElse(0);
        status.setOrderIndex(nextOrder);
        Status savedStatus = statusRepository.save(status);
        return statusMapper.toDto(savedStatus);
    }

    public StatusDto updateStatus(Long id, StatusDto statusDto) {
        User currentUser = getCurrentUser();
        Status status = statusRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new StatusNotFoundException(id));
        status.setName(statusDto.getName());
        if (statusDto.getOrder() != null) {
            status.setOrderIndex(Math.max(0, statusDto.getOrder()));
        }
        Status updatedStatus = statusRepository.save(status);
        return statusMapper.toDto(updatedStatus);
    }

    public StatusDto findByName(String name) {
        User currentUser = getCurrentUser();
        Status status = statusRepository.findByNameAndUser(name, currentUser)
                .orElseThrow(() -> new StatusNotFoundException(name));
        return statusMapper.toDto(status);
    }

    @Override
    public StatusDto delete(Long id) {
        User currentUser = getCurrentUser();
        Status status = statusRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new StatusNotFoundException(id));
        statusRepository.delete(status);
        return statusMapper.toDto(status);
    }

    @Override
    public Status findByIdInternal(Long id) {
        User currentUser = getCurrentUser();
        return statusRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new StatusNotFoundException(id));
    }

    @Override
    public List<StatusDto> findAll() {
        User currentUser = getCurrentUser();
        List<StatusDto> statuses = statusRepository.findByUserOrderByOrderIndexAscNameAsc(currentUser).stream()
                .map(statusMapper::toDto)
                .sorted(Comparator.comparing((StatusDto dto) -> dto.getOrder() == null ? 0 : dto.getOrder())
                        .thenComparing(StatusDto::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        AtomicInteger orderCounter = new AtomicInteger();
        statuses.forEach(dto -> dto.setOrder(orderCounter.getAndIncrement()));
        return statuses;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден: " + username));
    }
}