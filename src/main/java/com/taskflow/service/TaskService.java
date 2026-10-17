package com.taskflow.service;

import com.taskflow.entity.Task;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public List<Task> findAllTasks() {
        return taskRepository.findAll();
    }

    // A plain RuntimeException becomes HTTP 500. ResponseStatusException
    // tells Spring to return 404 when the id is not in the table.
    public Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found with id: " + id));
    }

    public Task createTask(Task task) {
        requireTitle(task);
        task.setId(null);
        task.setTitle(task.getTitle().trim());
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        if (task.getPriority() == null) {
            task.setPriority(TaskPriority.MEDIUM);
        }
        return taskRepository.save(task);
    }

    public Task updateTask(Long id, Task updates) {
        Task existing = findTaskById(id);
        requireTitle(updates);
        existing.setTitle(updates.getTitle().trim());
        existing.setDescription(updates.getDescription());
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        if (updates.getPriority() != null) {
            existing.setPriority(updates.getPriority());
        }
        // dueDate is optional. Leave the stored date alone when the client omits it.
        if (updates.getDueDate() != null) {
            existing.setDueDate(updates.getDueDate());
        }
        return taskRepository.save(existing);
    }

    public void deleteTask(Long id) {
        Task existing = findTaskById(id);
        taskRepository.delete(existing);
    }

    private void requireTitle(Task task) {
        if (task == null || task.getTitle() == null || task.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
    }
}
