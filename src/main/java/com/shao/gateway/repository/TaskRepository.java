package com.shao.gateway.repository;

import com.shao.gateway.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    Optional<Task> findById(int id);

    @Modifying
    @Query(value = "update Task t set t.status= '111' where t.id= 1")
    int updateStatusById(Integer id, String status);
}
