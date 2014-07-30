package com.tapstream.rollbar.logback;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private final URL url;
    private final Map<String, String> requestProperties;
    private String method;
    private byte[] body;

    public HttpRequest(URL url, String method) {
        this.url = url;
        this.method = method;
        this.requestProperties = new HashMap<String, String>();
    }
    
    public URL getUrl(){
        return url;
    }
    
    public String getMethod(){
        return method;
    }

    public void setHeader(String key, String value) {
        requestProperties.put(key, value);
    }
    
    public Map<String, String> getHeaders(){
        return this.requestProperties;
    }

    public void setBody(String body) {
        try {
            this.body = body.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            this.body = body.getBytes();
        }
    }
    
    public byte[] getBody(){
        return this.body;
    }

}
