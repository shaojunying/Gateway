package com.shao.gateway.service;

import com.shao.gateway.entity.Interface;
import com.shao.gateway.entity.RawResponseEntity;
import com.shao.gateway.entity.MyResponseEntity;
import com.shao.gateway.entity.Task;
import com.shao.gateway.repository.InterfaceRepository;
import com.shao.gateway.repository.TaskRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Enumeration;
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
            return new MyResponseEntity(404, "Interface " + interfaceName + " not found", null);
        }

        if (interfaceRepository.increaseCurThreads(anInterface.getId()) == 0) {
            // 请求数量超过最大值
            return new MyResponseEntity(403, "Interface " + interfaceName + " is busy", null);
        }

        String url = extractUrl(request);
        String headers = extractHeaders(request);
        String body = extractBody(request);

        // 创建一条Task
        Task task = new Task(anInterface.getId(), "Running", url, headers, body, new Timestamp(System.currentTimeMillis()));
        final Task savedTask = taskRepository.save(task);

        // 这里如果在子线程中获取，则可能由于当前线程被销毁，而导致程序运行失败
        HttpHeaders httpHeaders = new HttpHeaders();
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            httpHeaders.add(headerName, request.getHeader(headerName));
        });
        HttpEntity<String> httpEntity = new HttpEntity<>(body, httpHeaders);

        if (anInterface.isSynchronous()) {
            // 同步接口
            executor.submit(new Thread(() -> {
                // 向http://localhost:8080/synchronous-task/submit发起HTTP请求
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<RawResponseEntity> responseEntity = restTemplate.postForEntity(anInterface.getRunUrl() + "?taskId=" + savedTask.getId(), httpEntity, RawResponseEntity.class);
                RawResponseEntity rawResponseEntity = responseEntity.getBody();
                if (rawResponseEntity.getCode() != 200) {
                    // 服务端错误
                    savedTask.setStatus("Failed");
                    savedTask.setEndTime(new Timestamp(System.currentTimeMillis()));
                    savedTask.setResult(rawResponseEntity.getData());
                    taskRepository.save(savedTask);
                    interfaceRepository.decreaseCurThreads(anInterface.getId());
                    return;
                }
                // 任务被正常执行了
                savedTask.setResult(rawResponseEntity.getData());
                savedTask.setEndTime(new Timestamp(System.currentTimeMillis()));
                try {
                    JSONObject jsonObject = new JSONObject(rawResponseEntity.getData());
                    savedTask.setStatus(jsonObject.getString("status"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                taskRepository.save(savedTask);

                interfaceRepository.decreaseCurThreads(anInterface.getId());
            }));
        } else {
            // 异步接口
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(anInterface.getSubmitUrl() + "?taskId=" + savedTask.getId(), httpEntity, String.class);
        }
        return new MyResponseEntity(200, "Running", "{taskId: " + savedTask.getId() + "}");
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
            return new MyResponseEntity(200, "Success to run task " + taskId, task.getResult());
        }

        if (task.getStatus().equals("Failed")) {
            return new MyResponseEntity(500, "Failed to run task " + taskId, task.getResult());
        }
        // 处于running状态
        // 判断是否超时


        if (anInterface.get().isSynchronous()) {
            if (task.getStartTime().getTime() + anInterface.get().getTimeout() < System.currentTimeMillis()) {
                task.setStatus("Failed");
                task.setEndTime(new Timestamp(System.currentTimeMillis()));
                taskRepository.save(task);
                return new MyResponseEntity(500, "Failed to run task " + taskId, task.getResult());
            } else {
                return new MyResponseEntity(200, "Task " + taskId + " is running", null);
            }
        } else {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<RawResponseEntity> responseEntity = restTemplate.getForEntity(anInterface.get().getCheckUrl() + "?taskId=" + task.getId(), RawResponseEntity.class);
            RawResponseEntity rawResponseEntity = responseEntity.getBody();
            if (rawResponseEntity == null) {
                return new MyResponseEntity(500, "Failed to run task " + taskId, null);
            }
            if (rawResponseEntity.getCode() != 200) {
                // 运行错误
                task.setStatus("Failed");
                task.setEndTime(new Timestamp(System.currentTimeMillis()));
                task.setResult(rawResponseEntity.getData());
                taskRepository.save(task);
                interfaceRepository.decreaseCurThreads(anInterface.get().getId());
                return new MyResponseEntity(500, "Failed to run task " + taskId, rawResponseEntity.getData());
            }
            // 任务被正常执行了
            task.setResult(rawResponseEntity.getData());
            task.setEndTime(new Timestamp(System.currentTimeMillis()));
            try {
                JSONObject jsonObject = new JSONObject(rawResponseEntity.getData());
                task.setStatus(jsonObject.getString("status"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            if (task.getStatus().equals("Running")) {
                if (task.getStartTime().getTime() + anInterface.get().getTimeout() < System.currentTimeMillis()) {
                    task.setStatus("Failed");
                    taskRepository.save(task);
                    interfaceRepository.decreaseCurThreads(anInterface.get().getId());
                    return new MyResponseEntity(500, "Failed to run task " + taskId, task.getResult());
                } else {
                    return new MyResponseEntity(200, "Task " + taskId + " is running", null);
                }
            }
            // Success
            taskRepository.save(task);

            interfaceRepository.decreaseCurThreads(anInterface.get().getId());
        }
        return new MyResponseEntity(200, task.getStatus(), task.getResult());
    }

    public MyResponseEntity markAsSuccess(int taskId, HttpServletRequest request) throws IOException {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return new MyResponseEntity(404, "Task not found", null);
        }
        if (!task.getStatus().equals("Running")) {
            return new MyResponseEntity(400, "Task is not running", null);
        }
        Optional<Interface> anInterface = interfaceRepository.findInterfaceById(task.getInterfaceId());
        if (anInterface.isEmpty()) {
            return new MyResponseEntity(404, "Interface not found", null);
        }
        task.setStatus("Success");
        task.setEndTime(new Timestamp(System.currentTimeMillis()));
        task.setResult(extractBody(request));
        taskRepository.save(task);
        interfaceRepository.decreaseCurThreads(anInterface.get().getId());
        return new MyResponseEntity(200, "Success to mark task " + taskId + " as success", null);
    }
}
