package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateLabelRequest;
import com.example.taskmanager.dto.response.LabelResponse;

import java.util.List;

public interface LabelService {
    LabelResponse createLabel(CreateLabelRequest request);
    List<LabelResponse> getAllLabels();
}
