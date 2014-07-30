package com.ahaid.rollbar.logback;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.ahaid.rollbar.logback.HttpRequest;
import com.ahaid.rollbar.logback.RollbarAppender;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class TestRollbarAppender {
    
    String apiKey = "api key";
    String endpoint = "http://rollbar.endpoint/";
    String env = "test";
    
    Logger rootLogger;
    LoggerContext loggerContext;
    RollbarAppender appender;
    MockHttpRequester httpRequester;
    
    @Before
    public void setup() {
        httpRequester = new MockHttpRequester();
        
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        loggerContext = rootLogger.getLoggerContext();
        loggerContext.reset();
                
        appender = new RollbarAppender();
        appender.setUrl(endpoint);
        appender.setApiKey(apiKey);
        appender.setEnvironment(env);
        appender.setAsync(false);
        appender.setHttpRequester(httpRequester);
        appender.setContext(loggerContext);
        appender.start();
        assertTrue(appender.isStarted());
        rootLogger.addAppender(appender);        
    }
    
    @After
    public void teardown(){
        
    }
    
    private void checkCommonRequestFields(HttpRequest request){
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals(endpoint, request.getUrl().toString());
    }
    
    @Test
    public void testMessage() throws Exception {
        String testMsg = "test";
        rootLogger.info(testMsg);
        HttpRequest request  = httpRequester.getRequest();
        checkCommonRequestFields(request);
        
        JSONObject root = new JSONObject(new String(request.getBody()));
        assertEquals(apiKey, root.get("access_token"));
        
        JSONObject data = root.getJSONObject("data");
        assertEquals(env, data.get("environment"));
        assertEquals("info", data.get("level"));
        assertEquals("java", data.get("platform"));
        assertEquals("java", data.get("language"));
        assertEquals("java", data.get("framework"));
        
        JSONObject body = data.getJSONObject("body");
        assertEquals(testMsg, body.getJSONObject("message").get("body"));
    }
    
    @Test
    public void testMessageSendError() throws Exception {
        String testMsg = "test";
        httpRequester.setResponseCode(500);
        rootLogger.info(testMsg);
    }
    
    @Test
    public void testThrowable() throws Exception {
        String testMsg = "test error";
        String testThrowableMsg = "test throwable";
        Throwable throwable = new Exception(testThrowableMsg);
        rootLogger.error(testMsg, throwable);
        HttpRequest request  = httpRequester.getRequest();
        checkCommonRequestFields(request);
        
        JSONObject root = new JSONObject(new String(request.getBody()));
        assertEquals(apiKey, root.get("access_token"));
        
        JSONObject data = root.getJSONObject("data");
        assertEquals(env, data.get("environment"));
        assertEquals("error", data.get("level"));
        assertEquals("java", data.get("platform"));
        assertEquals("java", data.get("language"));
        assertEquals("java", data.get("framework"));
    
        JSONObject body = data.getJSONObject("body");
        JSONArray traceChain = body.getJSONArray("trace_chain");
        JSONObject firstTrace = traceChain.getJSONObject(0);
        JSONArray frames = firstTrace.getJSONArray("frames");
        JSONObject lastFrame = frames.getJSONObject(frames.length() - 1);
        assertEquals("TestRollbarAppender.java", lastFrame.get("filename"));
        assertEquals("testThrowable", lastFrame.get("method"));
        assertEquals("com.tapstream.logback.TestRollbarAppender", lastFrame.get("class_name"));
        JSONObject firstException = firstTrace.getJSONObject("exception");
        assertEquals(testThrowableMsg, firstException.get("message"));
        assertEquals("java.lang.Exception", firstException.get("class"));
        
        JSONObject custom = data.getJSONObject("custom");
        assertEquals(testMsg, custom.get("log"));
    }
    
    
    

}
