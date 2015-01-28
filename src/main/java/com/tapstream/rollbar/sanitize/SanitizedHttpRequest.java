package com.tapstream.rollbar.sanitize;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


/**
 * Provides {@link HttpServletRequest} with sensitive information removed or obfuscated.
 */
public class SanitizedHttpRequest extends HttpServletRequestWrapper {
    private final HeaderSanitizer headerSanitizer;

    private final Map<String, String> sanitizedHeaders;

    private final HttpServletRequest originalRequest;

    public SanitizedHttpRequest(HttpServletRequest request, HeaderSanitizer headerSanitizer) {
        super(request);
        this.originalRequest = request;
        this.headerSanitizer = headerSanitizer;
        this.sanitizedHeaders = sanitizeHeaders();
    }

    @Override
    public String getHeader(String name) {
        return sanitizedHeaders.get(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(sanitizedHeaders.keySet());
    }

    private Map<String, String> sanitizeHeaders() {
        Map<String, String> newHeaders = new HashMap<String, String>();
        for (Enumeration<String> headerNames = originalRequest.getHeaderNames(); headerNames.hasMoreElements();) {
            String name = headerNames.nextElement();
            sanitizeHeader(name, originalRequest.getHeader(name), newHeaders);
        }
        return newHeaders;
    }

    private void sanitizeHeader(String name, String originalValue, Map<String, String> newHeaders) {
        String value = headerSanitizer.sanitize(name, originalValue);
        if (value != null) {
            newHeaders.put(name, value);
        }
    }

    @Override
    public Cookie[] getCookies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getDateHeader(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIntHeader(String name) {
        throw new UnsupportedOperationException();
    }
}
