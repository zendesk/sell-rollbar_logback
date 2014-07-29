package com.ahaid.rollbar.logback;

import java.io.IOException;

public class MockHttpRequester implements IHttpRequester{

    private int responseCode = 200;
    private HttpRequest request;

    @Override
    public int send(HttpRequest request) throws IOException {
        setRequest(request);
        return getResponseCode();
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }
}
