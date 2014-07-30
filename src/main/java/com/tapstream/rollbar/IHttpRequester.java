package com.tapstream.rollbar;

import java.io.IOException;

public interface IHttpRequester {
    
    public int send(HttpRequest request) throws IOException;

}
