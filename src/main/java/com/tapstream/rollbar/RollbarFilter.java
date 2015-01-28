package com.tapstream.rollbar;

import com.tapstream.rollbar.sanitize.HeaderSanitizer;
import com.tapstream.rollbar.sanitize.NoOpHeaderSanitizer;
import com.tapstream.rollbar.sanitize.SanitizedHttpRequest;

import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;


public class RollbarFilter implements Filter {
    
    public static final String REQUEST_PREFIX = "request.";
    public static final String REQUEST_REMOTE_ADDR = REQUEST_PREFIX + "remote_addr";
    public static final String REQUEST_URL = REQUEST_PREFIX + "url";
    public static final String REQUEST_QS = REQUEST_PREFIX + "qs";
    public static final String REQUEST_METHOD = REQUEST_PREFIX + "method";
    public static final String REQUEST_USER_AGENT = REQUEST_PREFIX + "user_agent";
    public static final String REQUEST_HEADER_PREFIX = REQUEST_PREFIX + "header.";
    public static final String REQUEST_PARAM_PREFIX = REQUEST_PREFIX + "param.";
    
    private final HeaderSanitizer headerSanitizer;

    public RollbarFilter() {
        this(new NoOpHeaderSanitizer());
    }

    public RollbarFilter(HeaderSanitizer headerSanitizer) {
        Objects.requireNonNull(headerSanitizer);
        
        this.headerSanitizer = headerSanitizer;
    }
    
    @Override
    public void init(FilterConfig config) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        Map<String, String> details = collectDetails(servletRequest);
        putToMDC(details);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            clearMDC(details);
        }

    }

    Map<String, String> collectDetails(ServletRequest request){
        Map<String, String> details = new HashMap<>();
        details.put(REQUEST_REMOTE_ADDR, request.getRemoteAddr());
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            collectHttpDetails(new SanitizedHttpRequest(httpRequest, headerSanitizer), details);
        }
        
        return details;
    }

    private void collectHttpDetails(HttpServletRequest httpRequest, Map<String, String> details) {
        String requestUrl = Objects.toString(httpRequest.getRequestURL(), null);
        if (requestUrl != null) {
            details.put(REQUEST_URL, requestUrl);
        }
        details.put(REQUEST_QS, httpRequest.getQueryString());
        details.put(REQUEST_METHOD, httpRequest.getMethod());
        details.put(REQUEST_USER_AGENT, httpRequest.getHeader("User-Agent"));
        
        for (Enumeration<String> headerNames = httpRequest.getHeaderNames(); headerNames.hasMoreElements(); ){
            String headerName = headerNames.nextElement();
            String headerValue = httpRequest.getHeader(headerName);
            details.put(REQUEST_HEADER_PREFIX + headerName, headerValue);
        }
        
        for (Enumeration<String> paramNames = httpRequest.getParameterNames(); paramNames.hasMoreElements(); ){
            String paramName = paramNames.nextElement();
            String paramValue = httpRequest.getParameter(paramName);
            details.put(REQUEST_PARAM_PREFIX + paramName, paramValue);
        }
    }
    
    void putToMDC(Map<String, String> details) {
        for(Entry<String,String> entry : details.entrySet()) {
            MDC.put(entry.getKey(), entry.getValue());
        }
    }
    
    void clearMDC(Map<String, String> details){
        for(String key : details.keySet()) {
            MDC.remove(key);
        }
    }

    @Override
    public void destroy() {}

}
