package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.request.CreateLabelRequest;
import com.example.taskmanager.dto.response.LabelResponse;
import com.example.taskmanager.entity.Label;
import com.example.taskmanager.exception.ConflictException;
import com.example.taskmanager.mapper.LabelMapper;
import com.example.taskmanager.repository.LabelRepository;
import com.example.taskmanager.service.LabelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LabelServiceImpl implements LabelService {

    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;

    public LabelServiceImpl(LabelRepository labelRepository, LabelMapper labelMapper) {
        this.labelRepository = labelRepository;
        this.labelMapper = labelMapper;
    }

    @Override
    public LabelResponse createLabel(CreateLabelRequest request) {
        if (labelRepository.existsByName(request.getName())) {
            throw new ConflictException("Label name already exists: " + request.getName());
        }

        Label label = labelMapper.toEntity(request);
        Label savedLabel = labelRepository.save(label);
        return labelMapper.toResponse(savedLabel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelResponse> getAllLabels() {
        return labelRepository.findAll().stream()
                .map(labelMapper::toResponse)
                .collect(Collectors.toList());
    }
}
