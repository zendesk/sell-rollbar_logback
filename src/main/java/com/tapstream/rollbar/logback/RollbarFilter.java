package com.tapstream.rollbar.logback;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.MDC;


public class RollbarFilter implements Filter {
    
    public static final String REQUEST_PREFIX = "request.";
    public static final String REQUEST_REMOTE_ADDR = REQUEST_PREFIX + "remote_addr";
    public static final String REQUEST_URL = REQUEST_PREFIX + "url";
    public static final String REQUEST_QS = REQUEST_PREFIX + "qs";
    public static final String REQUEST_METHOD = REQUEST_PREFIX + "method";
    public static final String REQUEST_USER_AGENT = REQUEST_PREFIX + "user_agent";
    public static final String REQUEST_HEADER_PREFIX = REQUEST_PREFIX + "header.";
    public static final String REQUEST_PARAM_PREFIX = REQUEST_PREFIX + "param.";

    @Override
    public void init(FilterConfig config) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        insertIntoMDC(servletRequest);

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            clearMDC();
        }

    }
    
    void insertIntoMDC(ServletRequest request){
        
        MDC.put(REQUEST_REMOTE_ADDR, request.getRemoteAddr());
        
        if (request instanceof HttpServletRequest){
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            StringBuffer requestUrl = httpRequest.getRequestURL();
            if (requestUrl != null){
                MDC.put(REQUEST_URL, requestUrl.toString());
            }
            MDC.put(REQUEST_QS, httpRequest.getQueryString());
            MDC.put(REQUEST_METHOD, httpRequest.getMethod());
            MDC.put(REQUEST_USER_AGENT, httpRequest.getHeader("User-Agent"));
            
            for (Enumeration<String> headerNames = httpRequest.getHeaderNames(); headerNames.hasMoreElements(); ){
                String headerName = headerNames.nextElement();
                String headerValue = httpRequest.getHeader(headerName);
                MDC.put(REQUEST_HEADER_PREFIX + headerName, headerValue);
            }
            
            for (Enumeration<String> paramNames = httpRequest.getParameterNames(); paramNames.hasMoreElements(); ){
                String paramName = paramNames.nextElement();
                String paramValue = httpRequest.getParameter(paramName);
                MDC.put(REQUEST_PARAM_PREFIX + paramName, paramValue);
            }
        }
    }
    
    void clearMDC(){
        MDC.clear();
    }

    @Override
    public void destroy() {}

}
