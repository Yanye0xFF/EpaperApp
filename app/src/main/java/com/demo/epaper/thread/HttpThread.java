package com.demo.epaper.thread;

import com.demo.epaper.handler.HttpCallback;
import com.demo.epaper.network.HttpUtil;

public class HttpThread extends Thread {

    private int id;
    private final String url;
    private final HttpCallback callback;

    public HttpThread(int id, String url, HttpCallback callback) {
        this.id = id;
        this.url = url;
        this.callback = callback;
    }

    @Override
    public void run() {
        HttpUtil httpUtil = HttpUtil.getInstance();
        int result = httpUtil.doGet(url, null);
        if(result == HttpUtil.STATE_OK) {
            callback.onResponse(id, result, httpUtil.getBody());
        }else {
            callback.onResponse(id, result, null);
        }
    }
}
