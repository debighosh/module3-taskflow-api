package com.taskflow.config;

import com.taskflow.entity.Task;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TaskDataSeeder implements ApplicationRunner {

    private final TaskRepository taskRepository;

    public TaskDataSeeder(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // H2 starts empty every launch, so this fills the list for local dev and tests.
        // Postgres on Railway keeps its rows, so skip seeding when anything is already stored.
        // data.sql would insert again on every restart and duplicate tasks.
        if (taskRepository.count() > 0) {
            return;
        }

        taskRepository.saveAll(List.of(
                task(
                        "Set up the project repository",
                        "Create the Git repository, add a README, and protect the main branch.",
                        TaskStatus.DONE,
                        TaskPriority.HIGH,
                        LocalDate.of(2026, 10, 20)),
                task(
                        "Draft the API contract",
                        "Agree the task JSON fields and the list, create, update, and delete endpoints.",
                        TaskStatus.IN_PROGRESS,
                        TaskPriority.HIGH,
                        LocalDate.of(2026, 10, 24)),
                task(
                        "Design the task board layout",
                        "Sketch a responsive board with columns for to do, in progress, and done.",
                        TaskStatus.IN_PROGRESS,
                        TaskPriority.MEDIUM,
                        LocalDate.of(2026, 10, 28)),
                task(
                        "Write endpoint documentation",
                        "Document each REST endpoint with a request example and a response example.",
                        TaskStatus.TODO,
                        TaskPriority.LOW,
                        null),
                task(
                        "Add email reminders",
                        "Send a short reminder when a task is still open on its due date.",
                        TaskStatus.TODO,
                        TaskPriority.MEDIUM,
                        LocalDate.of(2026, 11, 5)),
                task(
                        "Support drag and drop ordering",
                        "Let someone move a task within a column and across status columns.",
                        TaskStatus.TODO,
                        TaskPriority.LOW,
                        null)
        ));
    }

    private Task task(String title, String description, TaskStatus status,
                      TaskPriority priority, LocalDate dueDate) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueDate(dueDate);
        return task;
    }
}
