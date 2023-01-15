package com.shao.gateway.service;

import com.shao.gateway.entity.Interface;
import com.shao.gateway.entity.RawResponseEntity;
import com.shao.gateway.entity.MyResponseEntity;
import com.shao.gateway.entity.Task;
import com.shao.gateway.repository.InterfaceRepository;
import com.shao.gateway.repository.TaskRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final InterfaceRepository interfaceRepository;

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            10,
            20,
            0L,
            java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.LinkedBlockingQueue<Runnable>()
    );

    private final TaskRepository taskRepository;

    public TaskService(InterfaceRepository interfaceRepository, TaskRepository taskRepository) {
        this.interfaceRepository = interfaceRepository;
        this.taskRepository = taskRepository;
    }


    public MyResponseEntity submit(HttpServletRequest request, String interfaceName) throws IOException {
        Interface anInterface = interfaceRepository.findInterfaceByName(interfaceName).orElse(null);
        if (anInterface == null) {
            return new MyResponseEntity(404, "interface not found", null);
        }

        // 是否超过了最大并发数
        // TODO 这里应该++curThreads
        if (anInterface.getMaxThreads() <= anInterface.getCurThreads()) {
            return new MyResponseEntity(403, "too many requests", null);
        }

        // TODO cookie
        String url = extractUrl(request);
        String headers = extractHeaders(request);
        String body = extractBody(request);

        // 创建一条Task
        Task task = new Task(anInterface.getId(), "Running", url, headers, body, new Timestamp(System.currentTimeMillis()));
        final Task savedTask = taskRepository.save(task);

        // 这里如果在子线程中获取，则可能由于当前线程被销毁，而导致程序运行失败
        HttpHeaders httpHeaders = new HttpHeaders();
        System.out.println("122");
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            System.out.println("headerName: " + headerName);
            httpHeaders.add(headerName, request.getHeader(headerName));
        });
        HttpEntity<String> httpEntity = new HttpEntity<>(body, httpHeaders);

        if (anInterface.isSynchronous()) {
            // 同步接口
            executor.submit(new Thread(() -> {
                // 向http://localhost:8080/synchronous-task/submit发起HTTP请求
                RestTemplate restTemplate = new RestTemplate();
                // TODO 考虑异常情况
                ResponseEntity<RawResponseEntity> responseEntity = restTemplate.postForEntity(anInterface.getRunUrl() + "?taskId=" + savedTask.getId(), httpEntity, RawResponseEntity.class);
                RawResponseEntity rawResponseEntity = responseEntity.getBody();
                savedTask.setStatus(rawResponseEntity.getStatus());
                savedTask.setResult(rawResponseEntity.getData());
                savedTask.setEndTime(new Timestamp(System.currentTimeMillis()));
                taskRepository.save(savedTask);
            }));
        }else {
            // 异步接口
            RestTemplate restTemplate = new RestTemplate();
            // TODO 考虑异常情况
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(anInterface.getSubmitUrl() + "?taskId=" + savedTask.getId(), httpEntity, String.class);
        }
        return new MyResponseEntity(201, "Running", "{taskId: " + savedTask.getId() + "}");
    }


    private String extractUrl(HttpServletRequest request) {
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }

    private String extractHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.append(headerName).append(":").append(request.getHeader(headerName)).append(";");
        }
        return headers.toString();
    }

    private String extractBody(HttpServletRequest request) throws IOException {
        // 这里的reader只能被读取一次
        return request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    }

    public MyResponseEntity check(int taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return new MyResponseEntity(404, "Task not found", null);
        }
        Optional<Interface> anInterface = interfaceRepository.findInterfaceById(task.getInterfaceId());
        if (anInterface.isEmpty()) {
            return new MyResponseEntity(404, "Interface not found", null);
        }

        // 如果成功或失败则直接返回
        if (task.getStatus().equals("Success")) {
            return new MyResponseEntity(200, "Success", task.getResult());
        }

        if (task.getStatus().equals("Failed")) {
            return new MyResponseEntity(500, "Failed", task.getResult());
        }

        // 处于running状态，并且是异步接口 （同步接口继续等待即可）
        if (anInterface.get().isSynchronous()) {
            return new MyResponseEntity(201, "Running", null);
        }

        // 处于running状态，并且是异步接口
        RestTemplate restTemplate = new RestTemplate();
        // TODO 考虑异常情况
        ResponseEntity<RawResponseEntity> responseEntity = restTemplate.getForEntity(anInterface.get().getCheckUrl() + "?taskId=" + task.getId(), RawResponseEntity.class);
        RawResponseEntity rawResponseEntity = responseEntity.getBody();
        if (rawResponseEntity == null) {
            return new MyResponseEntity(500, "Failed", null);
        }
        if (Objects.equals(rawResponseEntity.getStatus(), "Success")) {
            task.setStatus("Success");
            task.setResult(rawResponseEntity.getData());
            task.setEndTime(new Timestamp(System.currentTimeMillis()));
            taskRepository.save(task);
            return new MyResponseEntity(200, "Success", task.getResult());
        }
        if (Objects.equals(rawResponseEntity.getStatus(), "Failed")) {
            task.setStatus("Failed");
            task.setResult(rawResponseEntity.getData());
            task.setEndTime(new Timestamp(System.currentTimeMillis()));
            taskRepository.save(task);
            return new MyResponseEntity(500, "Failed", task.getResult());
        }
        return new MyResponseEntity(201, "Running", null);
    }

}
