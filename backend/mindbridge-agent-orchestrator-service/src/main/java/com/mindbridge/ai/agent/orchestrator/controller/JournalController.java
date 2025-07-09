package com.mindbridge.ai.agent.orchestrator.controller;

import com.mindbridge.ai.agent.orchestrator.models.dto.CreateJournalEntryRequest;
import com.mindbridge.ai.agent.orchestrator.models.dto.JournalEntryDto;
import com.mindbridge.ai.agent.orchestrator.models.dto.UpdateJournalEntryRequest;
import com.mindbridge.ai.agent.orchestrator.service.JournalService;
import com.mindbridge.ai.common.annotation.SysLog;
import com.mindbridge.ai.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/journal")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @SysLog("create journal entry")
    @PostMapping
    public ResponseEntity<JournalEntryDto> createJournalEntry(@Valid @RequestBody CreateJournalEntryRequest request) {
        JournalEntryDto journalEntry = journalService.createJournalEntry(request, SecurityUtils.getUserId());
        return ResponseEntity.ok(journalEntry);
    }

    @SysLog("get journal entries by page")
    @GetMapping
    public ResponseEntity<Page<JournalEntryDto>> getJournalEntries(@RequestParam(value = "page", defaultValue = "0", required = false) int pageNumber,
                                                                   @RequestParam(value = "size", defaultValue = "20", required = false) int pageSize) {
        Page<JournalEntryDto> entries = journalService.getJournalEntries(SecurityUtils.getUserId(),
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(entries);
    }

    @SysLog("get journal entries by code")
    @GetMapping("/{code}")
    public ResponseEntity<JournalEntryDto> getJournalEntry(@PathVariable String code) {
        JournalEntryDto entry = journalService.getJournalEntry(code, SecurityUtils.getUserId());
        return ResponseEntity.ok(entry);
    }

    @SysLog("update journal entry by code")
    @PutMapping("/{code}")
    public ResponseEntity<JournalEntryDto> updateJournalEntry(@PathVariable String code,
            @Valid @RequestBody UpdateJournalEntryRequest request) {
        JournalEntryDto entry = journalService.updateJournalEntry(code, request, SecurityUtils.getUserId());
        return ResponseEntity.ok(entry);
    }

    @SysLog("delete journal entry by code")
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteJournalEntry(@PathVariable String code) {
        journalService.deleteJournalEntry(code, SecurityUtils.getUserId());
        return ResponseEntity.noContent().build();
    }

    @SysLog("search journal entry by keyword")
    @GetMapping("/search")
    public ResponseEntity<List<JournalEntryDto>> searchJournalEntries(@RequestParam String query) {
        List<JournalEntryDto> entries = journalService.searchJournalEntries(query, SecurityUtils.getUserId());
        return ResponseEntity.ok(entries);
    }

}
