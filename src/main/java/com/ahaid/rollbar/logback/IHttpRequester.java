package com.ahaid.rollbar.logback;

import java.io.IOException;

public interface IHttpRequester {
    
    public int send(HttpRequest request) throws IOException;

}
