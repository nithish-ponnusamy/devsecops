package com.devsecops.app;

import com.devsecops.app.controller.TaskController;
import com.devsecops.app.model.Task;
import com.devsecops.app.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Test
    void healthCheck_ShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void getAllTasks_ShouldReturnTaskList() throws Exception {
        Task task1 = new Task("Task 1", "Description 1", "HIGH", "user1");
        Task task2 = new Task("Task 2", "Description 2", "LOW", "user2");

        when(taskService.getAllTasks()).thenReturn(Arrays.asList(task1, task2));

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getTaskById_WhenExists_ShouldReturnTask() throws Exception {
        Task task = new Task("Task 1", "Description 1", "HIGH", "user1");
        task.setId("abc123");

        when(taskService.getTaskById("abc123")).thenReturn(Optional.of(task));

        mockMvc.perform(get("/api/v1/tasks/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Task 1"));
    }

    @Test
    void getTaskById_WhenNotExists_ShouldReturn404() throws Exception {
        when(taskService.getTaskById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tasks/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTask_ShouldReturnCreatedTask() throws Exception {
        Task task = new Task("New Task", "New Description", "MEDIUM", "user1");
        task.setId("new123");

        when(taskService.createTask(any(Task.class))).thenReturn(task);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Task\",\"description\":\"New Description\",\"priority\":\"MEDIUM\",\"assignedTo\":\"user1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Task"));
    }
}
