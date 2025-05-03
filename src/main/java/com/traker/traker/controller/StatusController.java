package com.traker.traker.controller;

import com.traker.traker.controller.api.StatusControllerApi;
import com.traker.traker.dto.entity.StatusDto;
import com.traker.traker.entity.Status;
import com.traker.traker.service.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statuses")
public class StatusController implements StatusControllerApi {

    @Autowired
    private StatusService statusService;

    @Override
    public ResponseEntity<StatusDto> createStatus(@RequestBody StatusDto statusDto) {
        StatusDto createdStatus = statusService.createStatus(statusDto);
        return ResponseEntity.ok(createdStatus);
    }

    @Override
    public ResponseEntity<StatusDto> findByName(@PathVariable String name) {
        StatusDto status = statusService.findByName(name);
        return ResponseEntity.ok(status);
    }

    @Override
    public ResponseEntity<StatusDto> updateStatus(@PathVariable Long id, @RequestBody StatusDto statusDto) {
        StatusDto updatedStatus = statusService.updateStatus(id, statusDto);
        return ResponseEntity.ok(updatedStatus);
    }

    @Override
    public ResponseEntity<Void> deleteStatus(@PathVariable Long id) {
        statusService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<StatusDto>> getAllStatuses() {
        return ResponseEntity.ok(statusService.findAll());
    }

    @Override
    public ResponseEntity<Status> getStatusById(@PathVariable Long id) {
        return ResponseEntity.ok(statusService.findByIdInternal(id));
    }
}