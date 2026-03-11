package com.devsecops.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("application", "DevSecOps Task Manager");
        response.put("status", "UP");

        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("health", "GET /api/v1/tasks/health");
        endpoints.put("list_tasks", "GET /api/v1/tasks");
        endpoints.put("get_task", "GET /api/v1/tasks/{id}");
        endpoints.put("create_task", "POST /api/v1/tasks");
        endpoints.put("update_task", "PUT /api/v1/tasks/{id}");
        endpoints.put("delete_task", "DELETE /api/v1/tasks/{id}");
        endpoints.put("by_status", "GET /api/v1/tasks/status/{status}");
        endpoints.put("by_priority", "GET /api/v1/tasks/priority/{priority}");
        endpoints.put("by_assignee", "GET /api/v1/tasks/assignee/{name}");
        response.put("endpoints", endpoints);

        return ResponseEntity.ok(response);
    }
}
