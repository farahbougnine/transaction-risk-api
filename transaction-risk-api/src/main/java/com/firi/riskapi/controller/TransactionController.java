package com.firi.riskapi.controller;

import com.firi.riskapi.model.Transaction;
import com.firi.riskapi.repository.TransactionRepository;
import com.firi.riskapi.service.CsvImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final CsvImportService csvImportService;

    public TransactionController(TransactionRepository transactionRepository, CsvImportService csvImportService) {
        this.transactionRepository = transactionRepository;
        this.csvImportService = csvImportService;
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<CsvImportService.ImportResult> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        CsvImportService.ImportResult result = csvImportService.importCsv(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public List<Transaction> all() {
        return transactionRepository.findAll();
    }

    @GetMapping("/flagged")
    public List<Transaction> flagged() {
        return transactionRepository.findByFlaggedTrue();
    }

    @GetMapping("/account/{accountId}")
    public List<Transaction> byAccount(@PathVariable String accountId) {
        return transactionRepository.findByAccountIdOrderByTimestampDesc(accountId);
    }
}
