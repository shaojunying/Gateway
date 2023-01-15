package com.shao.gateway.controller;

import com.shao.gateway.entity.MyResponseEntity;
import com.shao.gateway.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController()
@RequestMapping(value = "task")
public class TaskController {

    @Autowired
    private final TaskService taskService;


    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping(value = "submit")
    public MyResponseEntity submit(HttpServletRequest request, @RequestParam("interfaceName") String interfaceName) throws IOException {
        return taskService.submit(request, interfaceName);
    }

    @GetMapping(value = "check")
    public MyResponseEntity check(@RequestParam("taskId") int taskId) {
        return taskService.check(taskId);
    }

}
