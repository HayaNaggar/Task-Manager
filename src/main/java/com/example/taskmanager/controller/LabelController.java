package com.example.taskmanager.controller;

import com.example.taskmanager.dto.request.CreateLabelRequest;
import com.example.taskmanager.dto.response.LabelResponse;
import com.example.taskmanager.service.LabelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PostMapping
    public ResponseEntity<LabelResponse> createLabel(@Valid @RequestBody CreateLabelRequest request) {
        LabelResponse response = labelService.createLabel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LabelResponse>> getAllLabels() {
        List<LabelResponse> labels = labelService.getAllLabels();
        return ResponseEntity.ok(labels);
    }
}
