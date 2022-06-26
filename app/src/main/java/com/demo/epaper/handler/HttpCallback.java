package com.demo.epaper.handler;

@FunctionalInterface
public interface HttpCallback {
    void onResponse(int id, int code, String body);
}
