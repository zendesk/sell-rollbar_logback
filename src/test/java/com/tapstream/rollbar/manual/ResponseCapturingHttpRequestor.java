package com.tapstream.rollbar.manual;

import com.tapstream.rollbar.HttpRequest;
import com.tapstream.rollbar.IHttpRequester;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map.Entry;

public class ResponseCapturingHttpRequestor implements IHttpRequester {
    private Deque<String> responses = new LinkedList<>();
    private int timeout = 5000;
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public int send(HttpRequest request) throws IOException{
        
        URL url = request.getUrl();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod(request.getMethod());
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            for (Entry<String, String> pair : request.getHeaders().entrySet()) {
                connection.setRequestProperty(pair.getKey(), pair.getValue());
            }

            byte[] body = request.getBody();
            if (body != null) {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                writeBody(body, connection);
                responses.add(readResponse(connection));
            }

            return connection.getResponseCode();
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        final InputStream tmpIn = conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream(); 
        try(InputStream in = tmpIn){ 
            return IOUtils.toString(in);
        }
    }
    
    private void writeBody(byte[] body, HttpURLConnection connection) throws IOException {
        try (OutputStream out = new BufferedOutputStream(connection.getOutputStream())){
            out.write(body);
        }
    }

    public Collection<String> getResponses() {
        return Collections.unmodifiableCollection(responses);
    }
    
    public String getLastResponse() {
        return responses.peekLast();
    }
}