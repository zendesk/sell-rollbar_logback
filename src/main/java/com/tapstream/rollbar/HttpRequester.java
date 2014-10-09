package com.tapstream.rollbar;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map.Entry;

public class HttpRequester implements IHttpRequester {

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
                writeBody(body, connection);
            }

            return connection.getResponseCode();
            
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }
    
    private void writeBody(byte[] body, HttpURLConnection connection) throws IOException {
        try (OutputStream out = new BufferedOutputStream(connection.getOutputStream())){
            out.write(body);
        }
    }

}
