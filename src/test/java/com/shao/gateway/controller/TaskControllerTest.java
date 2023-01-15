package com.shao.gateway.controller;

import com.shao.gateway.entity.MyResponseEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TaskControllerTest {
    /**
     * 提交一条异步任务，并执行成功
     */
    @Test
    void testSuccessfulAsyncTask() throws InterruptedException, JSONException {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/task/submit?interfaceName=a1";
        MyResponseEntity myResponseEntity = restTemplate.postForObject(url, null, MyResponseEntity.class);
        assertNotNull(myResponseEntity);
        assertEquals(201, myResponseEntity.getCode());
        assertEquals("Running", myResponseEntity.getStatus());
        assertNotNull(myResponseEntity.getData());
        String data = myResponseEntity.getData();
        JSONObject jsonObject = new JSONObject(data);
        int taskId = jsonObject.getInt("taskId");

        Thread.sleep(5000);

        String checkUrl = "http://localhost:8080/task/check?taskId=" + taskId;
        MyResponseEntity responseEntity = restTemplate.getForObject(checkUrl, MyResponseEntity.class);
        assertNotNull(responseEntity);
        System.out.println("responseEntity = " + responseEntity);
        assertEquals(200, responseEntity.getCode());
        assertEquals("Success", responseEntity.getStatus());
        assertNull(responseEntity.getData());
    }

    /**
     * 提交一条异步任务，并执行失败
     */
    @Test
    void testFailedAsyncTask() throws InterruptedException, JSONException {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/task/submit?interfaceName=a2";
        MyResponseEntity myResponseEntity = restTemplate.postForObject(url, null, MyResponseEntity.class);
        assertNotNull(myResponseEntity);
        assertEquals(201, myResponseEntity.getCode());
        assertEquals("Running", myResponseEntity.getStatus());
        assertNotNull(myResponseEntity.getData());
        String data = myResponseEntity.getData();
        JSONObject jsonObject = new JSONObject(data);
        int taskId = jsonObject.getInt("taskId");

        Thread.sleep(5000);

        String checkUrl = "http://localhost:8080/task/check?taskId=" + taskId;
        MyResponseEntity responseEntity = restTemplate.getForObject(checkUrl, MyResponseEntity.class);
        assertNotNull(responseEntity);
        System.out.println("responseEntity = " + responseEntity);
        assertEquals(500, responseEntity.getCode());
        assertEquals("Failed", responseEntity.getStatus());
        assertNull(responseEntity.getData());
    }


    /**
     * 提交一条同步任务，并执行成功
     */
    @Test
    void testSuccessfulSyncTask() throws InterruptedException, JSONException {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/task/submit?interfaceName=b1";
        MyResponseEntity myResponseEntity = restTemplate.postForObject(url, null, MyResponseEntity.class);
        assertNotNull(myResponseEntity);
        assertEquals(201, myResponseEntity.getCode());
        assertEquals("Running", myResponseEntity.getStatus());
        assertNotNull(myResponseEntity.getData());
        String data = myResponseEntity.getData();
        JSONObject jsonObject = new JSONObject(data);
        int taskId = jsonObject.getInt("taskId");

        Thread.sleep(5000);

        String checkUrl = "http://localhost:8080/task/check?taskId=" + taskId;
        MyResponseEntity responseEntity = restTemplate.getForObject(checkUrl, MyResponseEntity.class);
        assertNotNull(responseEntity);
        System.out.println("responseEntity = " + responseEntity);
        assertEquals(200, responseEntity.getCode());
        assertEquals("Success", responseEntity.getStatus());
        assertNull(responseEntity.getData());
    }

    /**
     * 提交一条同步任务，并执行失败
     */
    @Test
    void testFailedSyncTask() throws InterruptedException, JSONException {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/task/submit?interfaceName=b2";
        MyResponseEntity myResponseEntity = restTemplate.postForObject(url, null, MyResponseEntity.class);
        assertNotNull(myResponseEntity);
        assertEquals(201, myResponseEntity.getCode());
        assertEquals("Running", myResponseEntity.getStatus());
        assertNotNull(myResponseEntity.getData());
        String data = myResponseEntity.getData();
        JSONObject jsonObject = new JSONObject(data);
        int taskId = jsonObject.getInt("taskId");

        Thread.sleep(5000);

        String checkUrl = "http://localhost:8080/task/check?taskId=" + taskId;
        MyResponseEntity responseEntity = restTemplate.getForObject(checkUrl, MyResponseEntity.class);
        assertNotNull(responseEntity);
        System.out.println("responseEntity = " + responseEntity);
        assertEquals(500, responseEntity.getCode());
        assertEquals("Failed", responseEntity.getStatus());
        assertNull(responseEntity.getData());
    }
}