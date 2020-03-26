package com.xuehai.test.model;

import lombok.Data;

import java.util.Map;

/**
 * @ClassName Request
 * @Description: TODO
 * @Author Sniper
 * @Date 2019/10/31 13:32
 */
@Data
public class Request {
    private String url;
    private String method;
    private Map<String, String> header;
    private String body;

    public Request(String url, String method, Map<String, String> header, String body) {
        this.url = url;
        this.method = method;
        this.header = header;
        this.body = body;
    }

}