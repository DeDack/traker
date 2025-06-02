package com.traker.traker.controller;

import com.traker.traker.controller.api.StatusControllerApi;
import com.traker.traker.dto.StatusDto;
import com.traker.traker.entity.Status;
import com.traker.traker.service.StatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StatusController implements StatusControllerApi {

    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StatusDto> createStatus(@RequestBody StatusDto statusDto) {
        StatusDto createdStatus = statusService.createStatus(statusDto);
        return ResponseEntity.ok(createdStatus);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StatusDto> findByName(@PathVariable String name) {
        StatusDto status = statusService.findByName(name);
        return ResponseEntity.ok(status);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StatusDto> updateStatus(@PathVariable Long id, @RequestBody StatusDto statusDto) {
        StatusDto updatedStatus = statusService.updateStatus(id, statusDto);
        return ResponseEntity.ok(updatedStatus);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteStatus(@PathVariable Long id) {
        statusService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StatusDto>> getAllStatuses() {
        return ResponseEntity.ok(statusService.findAll());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Status> getStatusById(@PathVariable Long id) {
        return ResponseEntity.ok(statusService.findByIdInternal(id));
    }
}