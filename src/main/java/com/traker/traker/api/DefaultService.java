package com.traker.traker.api;

import com.traker.traker.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
@Transactional
public abstract class DefaultService<ID, ENT extends DefaultEntity, DTO> {
    DefaultRepository<ENT, ID> repository;
    DefaultMapper<ENT, DTO> mapper;
    Function<ID, NotFoundException> notFoundExceptionFunction;

    @Transactional(readOnly = true)
    public ENT findByIdInternal(ID id) {
        return repository.findById(id)
            .orElseThrow(() -> notFoundExceptionFunction.apply(id));
    }

    @Transactional(readOnly = true)
    public ENT findByExampleInternal(ENT entity) {
        return repository.findOne(
                Example.of(
                    entity,
                    ExampleMatcher.matching().withIgnoreNullValues()))
            .orElseThrow(() -> notFoundExceptionFunction.apply((ID) entity.getId()));
    }

    @Transactional(readOnly = true)
    public DTO findById(ID id) {
        return mapper.toDto(this.findByIdInternal(id));
    }

    @Transactional(readOnly = true)
    public DTO findByExample(DTO dto) {
        return mapper.toDto(this.findByExampleInternal(mapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public List<DTO> findAllByExample(DTO dto) {
        return repository.findAll(
                Example.of(
                    mapper.toEntity(dto),
                    ExampleMatcher.matching().withIgnoreNullValues()))
            .stream()
            .map(mapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<DTO> findAllByExample(DTO dto, Pageable pageable) {
        return
            repository.findAll(
                    Example.of(
                        mapper.toEntity(dto),
                        ExampleMatcher.matching().withIgnoreNullValues()),
                    pageable)
                .map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<DTO> findAll() {
        return repository.findAll().stream()
            .map(mapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<DTO> findAll(Pageable pageable) {
        return repository.findAll(pageable)
            .map(mapper::toDto);
    }

    public DTO update(ID id, DTO dto) {
        return mapper.toDto(
            repository.save(
                this.updateInternal(
                    this.findByIdInternal(id),
                    dto)));
    }

    public DTO delete(ID id) {
        repository.deleteById(id);
        return null;
    }

    protected ENT updateInternal(ENT ent, DTO update) {
        return mapper.toEntity(update);
    }

    @Transactional(readOnly = true)
    public DTO findByAttribute(Function<String, Optional<ENT>> findFunction, String attributeValue) {
        ENT entity = findFunction.apply(attributeValue)
                .orElseThrow(() -> notFoundExceptionFunction.apply((ID) attributeValue));
        return mapper.toDto(entity);
    }
}
