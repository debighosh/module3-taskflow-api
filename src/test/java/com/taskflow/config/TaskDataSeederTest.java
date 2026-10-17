package com.taskflow.config;

import com.taskflow.entity.Task;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskDataSeederTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskDataSeeder taskDataSeeder;

    @Test
    void seedsSixTasksWhenTheTableIsEmpty() {
        when(taskRepository.count()).thenReturn(0L);

        taskDataSeeder.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Task>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(taskRepository).saveAll(captor.capture());

        List<Task> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertEquals(6, saved.size());
    }

    @Test
    void doesNotSeedWhenTasksAlreadyExist() {
        when(taskRepository.count()).thenReturn(4L);

        taskDataSeeder.run(null);

        verify(taskRepository, never()).saveAll(any());
    }
}
