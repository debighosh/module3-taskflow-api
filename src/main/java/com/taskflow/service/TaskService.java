package com.taskflow.service;

import com.taskflow.entity.Task;
import com.taskflow.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    public List<Task> findAllTasks() {
        return taskRepository.findAll();
    }
    
    // TODO completed: look up by id, or fail clearly if missing
    public Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }
    
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }
}
