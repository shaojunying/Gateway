package com.shao.gateway.entity;

public class RawResponseEntity {
    private String status;
    private String data;

    public RawResponseEntity() {
    }

    public RawResponseEntity(String status, String data) {
        this.status = status;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public String getData() {
        return data;
    }
}
