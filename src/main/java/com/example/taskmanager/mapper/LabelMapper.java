package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.request.CreateLabelRequest;
import com.example.taskmanager.dto.response.LabelResponse;
import com.example.taskmanager.entity.Label;
import org.springframework.stereotype.Component;

@Component
public class LabelMapper {

    public Label toEntity(CreateLabelRequest request) {
        Label label = new Label();
        label.setName(request.getName());
        label.setColor(request.getColor());
        return label;
    }

    public LabelResponse toResponse(Label label) {
        return new LabelResponse(
                label.getId(),
                label.getName(),
                label.getColor()
        );
    }
}
