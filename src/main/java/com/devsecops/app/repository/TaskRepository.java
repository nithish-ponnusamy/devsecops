package com.devsecops.app.repository;

import com.devsecops.app.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByStatus(String status);

    List<Task> findByPriority(String priority);

    List<Task> findByAssignedTo(String assignedTo);
}
