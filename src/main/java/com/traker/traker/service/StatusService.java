package com.traker.traker.service;

import com.traker.traker.api.DefaultService;
import com.traker.traker.dto.StatusDto;
import com.traker.traker.entity.Status;
import com.traker.traker.exception.StatusNotFoundException;
import com.traker.traker.mapper.StatusMapper;
import com.traker.traker.repository.StatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatusService extends DefaultService<Long, Status, StatusDto> {

    private final StatusMapper statusMapper;
    private final StatusRepository statusRepository;

    @Autowired
    public StatusService(StatusMapper statusMapper, StatusRepository statusRepository) {
        super(statusRepository, statusMapper, StatusNotFoundException::new);
        this.statusMapper = statusMapper;
        this.statusRepository = statusRepository;
    }

    public StatusDto createStatus(StatusDto statusDto) {
        Status status = statusMapper.toEntity(statusDto);
        Status savedStatus = statusRepository.save(status);
        return statusMapper.toDto(savedStatus);
    }

    public StatusDto updateStatus(Long id, StatusDto statusDto) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new StatusNotFoundException(id));
        status.setName(statusDto.getName());
        Status updatedStatus = statusRepository.save(status);
        return statusMapper.toDto(updatedStatus);
    }

    public StatusDto findByName(String name) {
        return findByAttribute(statusRepository::findByName, name);
    }
}