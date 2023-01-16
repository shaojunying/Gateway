package com.shao.gateway.entity;

public class MyResponseEntity {
    private int code;
    private String message;
    private String data;

    public MyResponseEntity() {
    }

    public MyResponseEntity(int code, String message, String data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }

    public String getData() {
        return data;
    }
}
