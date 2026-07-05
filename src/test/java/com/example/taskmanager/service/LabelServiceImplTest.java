package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateLabelRequest;
import com.example.taskmanager.dto.response.LabelResponse;
import com.example.taskmanager.entity.Label;
import com.example.taskmanager.exception.ConflictException;
import com.example.taskmanager.mapper.LabelMapper;
import com.example.taskmanager.repository.LabelRepository;
import com.example.taskmanager.service.impl.LabelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

public class LabelServiceImplTest {

    @Mock
    private LabelRepository labelRepository;

    private LabelMapper labelMapper;
    private LabelServiceImpl labelService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.labelMapper = new LabelMapper();
        this.labelService = new LabelServiceImpl(labelRepository, labelMapper);
    }

    @Test
    public void createLabel_duplicateName_throwsConflict() {
        CreateLabelRequest request = new CreateLabelRequest();
        request.setName("Bug");
        request.setColor("#FF0000");

        Mockito.when(labelRepository.existsByName("Bug")).thenReturn(true);

        assertThrows(ConflictException.class, () -> labelService.createLabel(request));
        Mockito.verify(labelRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    public void createLabel_newName_succeeds() {
        CreateLabelRequest request = new CreateLabelRequest();
        request.setName("Enhancement");
        request.setColor("#00FF00");

        Mockito.when(labelRepository.existsByName("Enhancement")).thenReturn(false);
        Mockito.when(labelRepository.save(ArgumentMatchers.any(Label.class)))
                .thenAnswer(invocation -> {
                    Label l = invocation.getArgument(0);
                    l.setId(5L);
                    return l;
                });

        LabelResponse response = labelService.createLabel(request);

        assertNotNull(response);
        assertEquals("Enhancement", response.getName());
        assertEquals("#00FF00", response.getColor());
    }
}
