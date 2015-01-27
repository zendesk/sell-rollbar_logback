package com.tapstream.rollbar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.tapstream.rollbar.sanitize.HeaderSanitizer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.MDC;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class TestRollbarFilter {
    
    @Mock
    private HttpServletRequest request;
    @Mock
    private FilterConfig filterConfig;
    
    @Before
    @After
    public void before() {
        MDC.clear();
    }
    
    @Test
    public void initSucceeds() throws Exception {
        new RollbarFilter().init(filterConfig);
        
        // expecting no exception
    }
    @Test
    public void destroySucceeds() {
        new RollbarFilter().destroy();
        
        // expecting no exception 
    }
    @Test
    public void testFilter() throws Exception {
        RollbarFilter filter = new RollbarFilter();
        
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
        headers.put("User-Agent", ua);
        
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
        @SuppressWarnings("serial")
        Map<String, String> expected = new HashMap<String, String>(){{
            put(RollbarFilter.REQUEST_REMOTE_ADDR, remoteAddr);
            put(RollbarFilter.REQUEST_QS, qs);
            put(RollbarFilter.REQUEST_METHOD, method);
            put(RollbarFilter.REQUEST_USER_AGENT, ua);
        }};
        
        expected.put(RollbarFilter.REQUEST_HEADER_PREFIX + headerName, headerValue);
        expected.put(RollbarFilter.REQUEST_HEADER_PREFIX + "User-Agent", ua);
        expected.put(RollbarFilter.REQUEST_PARAM_PREFIX + paramName, paramValue);
        
        assertThat(actual, is(equalTo(expected)));
        
        filter.clearMDC();
        assertThat(MDC.getCopyOfContextMap(), is(nullValue()));
    }
    
    @Test
    public void headerValuesAreSanitized() {
        //given
        List<String> headerNames = new ArrayList<String>();
        headerNames.add("secureHeader");
        headerNames.add("otherHeader");
        headerNames.add("secureHeaderToRemove");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(headerNames));
        when(request.getHeader("secureHeader")).thenReturn("secret");
        when(request.getHeader("otherHeader")).thenReturn("otherValue");
        when(request.getHeader("secureHeaderToRemove")).thenReturn("secret2");
        when(request.getParameterNames()).thenReturn(Collections.enumeration(Collections.<String>emptyList()));
        
        HeaderSanitizer headerSanitizer = new HeaderSanitizer() {
            @Override
            public String sanitize(String headerName, String headerValue) {
                if(headerName.equals("secureHeader")) {
                    return "XXX";
                }else if(headerName.equals("otherHeader")) {
                    return headerValue;
                }else if(headerName.equals("secureHeaderToRemove")) {
                    return null;
                }else {
                    throw new IllegalArgumentException();
                }
            }
        };
        
        // when
        RollbarFilter filter = new RollbarFilter(headerSanitizer);
        filter.insertIntoMDC(request);
        
        // then
        Map<String, String> actual = MDC.getCopyOfContextMap();
        System.out.println(actual.toString());
        assertThat(actual.get("request.header.secureHeader"), is(equalTo("XXX")));
        assertThat(actual.get("request.header.otherHeader"), is(equalTo("otherValue")));
        assertFalse(actual.containsKey("request.header.secureHeaderToRemove"));
        
    }
    
}
