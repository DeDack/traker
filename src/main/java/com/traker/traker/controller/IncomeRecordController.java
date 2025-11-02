package com.traker.traker.controller;

import com.traker.traker.controller.api.IncomeRecordControllerApi;
import com.traker.traker.dto.income.IncomeBatchCreateRequestDto;
import com.traker.traker.dto.income.IncomeRecordResponseDto;
import com.traker.traker.dto.income.IncomeSummaryDto;
import com.traker.traker.service.IncomeRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IncomeRecordController implements IncomeRecordControllerApi {

    private final IncomeRecordService incomeRecordService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<IncomeRecordResponseDto>> createBatch(@RequestBody @Valid IncomeBatchCreateRequestDto request) {
        return ResponseEntity.ok(incomeRecordService.createBatch(request));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<IncomeRecordResponseDto>> getIncomes(@RequestParam(required = false) String from,
                                                                    @RequestParam(required = false) String to,
                                                                    @RequestParam(required = false) String month) {
        return ResponseEntity.ok(incomeRecordService.getIncomes(from, to, month));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IncomeSummaryDto> getSummary(@RequestParam(required = false) String from,
                                                       @RequestParam(required = false) String to,
                                                       @RequestParam(required = false) String month) {
        return ResponseEntity.ok(incomeRecordService.getSummary(from, to, month));
    }
}
