package com.muantech.rollbar.java;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

public class HttpRequest {

    private static final int REQUEST_TIMEOUT = 5000;

    private final URL url;

    private HttpURLConnection connection;
    private final HashMap<String, String> requestProperties;

    private String method;
    private byte[] body;

    private int attemptNumber;

    public HttpRequest(URL url, String method) {
        this.url = url;
        this.method = method;

        this.requestProperties = new HashMap<String, String>();

        attemptNumber = 0;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setRequestProperty(String key, String value) {
        requestProperties.put(key, value);
    }

    public void setBody(String body) {
        try {
            this.body = body.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            this.body = body.getBytes();
        }
    }

    public boolean execute() {

        attemptNumber++;

        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            // don't retry
            return true;
        }

        try {
            connection.setRequestMethod(this.method);
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setReadTimeout(REQUEST_TIMEOUT);

            for (Entry<String, String> pair : requestProperties.entrySet()) {
                connection.setRequestProperty(pair.getKey(), pair.getValue());
            }

            if (body != null) {
                connection.setDoOutput(true);
                writeBody(body, connection);
            }

            if (connection.getResponseCode() != 200) return false;

        } catch (IOException e) {
            // don't retry
            return true;
        } finally {
            connection.disconnect();
        }

        return true;
    }

    private void writeBody(byte[] body, HttpURLConnection connection) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(connection.getOutputStream());
            out.write(this.body);
        } finally {
            if (out != null) out.close();
        }

    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

}
