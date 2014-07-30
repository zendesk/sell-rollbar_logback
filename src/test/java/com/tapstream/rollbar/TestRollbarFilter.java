package com.tapstream.rollbar;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.collection.IsMapContaining;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import com.tapstream.rollbar.RollbarFilter;

public class TestRollbarFilter {

    
    /*
     *         MDC.put(REQUEST_REMOTE_ADDR, request.getRemoteAddr());
        
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
     */
    
    RollbarFilter filter;
    
    @Before
    public void setup() throws Exception {
        filter = new RollbarFilter();
        filter.init(mock(FilterConfig.class));
    }
    
    @After
    public void teardown() throws Exception {
        filter.destroy();
    }
    
    @Test
    public void testFilter() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        
        final String remoteAddr = "1.2.3.4";
        final String qs = "?qs=test";
        final String method = "GET";
        final String ua = "test user agent";
        final String headerName = "headerName";
        final String headerValue = "headerValue";
        final String paramName = "paramName";
        final String paramValue = "paramValue";
        
        Hashtable<String, String> headers = new Hashtable<>();
        headers.put(headerName, headerValue);
        
        Hashtable<String, String> params = new Hashtable<>();
        params.put(paramName, paramValue);
        
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(request.getQueryString()).thenReturn(qs);
        when(request.getMethod()).thenReturn(method);
        when(request.getHeader(argThat(equalToIgnoringCase("User-Agent")))).thenReturn(ua);
        when(request.getHeaderNames()).thenReturn(headers.keys());
        when(request.getHeader(argThat(equalToIgnoringCase(headerName)))).thenReturn(headerValue);
        when(request.getParameterNames()).thenReturn(params.keys());
        when(request.getParameter(paramName)).thenReturn(paramValue);

        filter.insertIntoMDC(request);
        
        Map<String, String> actual = MDC.getCopyOfContextMap();
        Map<String, String> expected = new HashMap<String, String>(){{
            put(RollbarFilter.REQUEST_REMOTE_ADDR, remoteAddr);
            put(RollbarFilter.REQUEST_QS, qs);
            put(RollbarFilter.REQUEST_METHOD, method);
            put(RollbarFilter.REQUEST_USER_AGENT, ua);
        }};
        
        expected.put(RollbarFilter.REQUEST_HEADER_PREFIX + headerName, headerValue);
        expected.put(RollbarFilter.REQUEST_PARAM_PREFIX + paramName, paramValue);
        
        assertThat(actual, is(equalTo(expected)));
        
        filter.clearMDC();
        assertThat(MDC.getCopyOfContextMap(), is(nullValue()));
    }
    
}
