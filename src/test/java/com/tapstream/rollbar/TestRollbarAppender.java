package com.tapstream.rollbar;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.entries;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    public void teardown() {

    }

    private void checkCommonRequestFields(HttpRequest request) {
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals(endpoint, request.getUrl().toString());
    }

    @Test
    public void testMessage() throws Exception {
        String testMsg = "test";
        rootLogger.info(testMsg);
        HttpRequest request = httpRequester.getRequest();
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
        HttpRequest request = httpRequester.getRequest();
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
        assertEquals("com.tapstream.rollbar.TestRollbarAppender", lastFrame.get("class_name"));
        JSONObject firstException = firstTrace.getJSONObject("exception");
        assertEquals(testThrowableMsg, firstException.get("message"));
        assertEquals("java.lang.Exception", firstException.get("class"));

        JSONObject custom = data.getJSONObject("custom");
        assertEquals(testMsg, custom.get("log"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStructuredArgumentsWithKeyValue() throws JSONException {
        rootLogger.error("some message", kv("k", "v"), kv("other-k", "other-v"));

        HttpRequest request = httpRequester.getRequest();
        checkCommonRequestFields(request);

        JSONObject root = new JSONObject(new String(request.getBody()));

        JSONObject data = (JSONObject) root.get("data");
        JSONObject custom = (JSONObject) data.get("custom");
        JSONObject args = (JSONObject) custom.get("args");

        assertThat(args.keys()).containsOnly("k", "other-k");
        assertThat(args.get("k")).isEqualTo("v");
        assertThat(args.get("other-k")).isEqualTo("other-v");
    }

    @Test
    public void testStructuredArgumentsWithEntries() throws JSONException {
        Map<String, Object> inner = new HashMap<>();
        inner.put("inner", "innervalue");
        Map<String, Object> outer = new HashMap<>();
        outer.put("outer", inner);

        rootLogger.error("some message", entries(outer));

        HttpRequest request = httpRequester.getRequest();
        checkCommonRequestFields(request);

        JSONObject root = new JSONObject(new String(request.getBody()));

        JSONObject data = (JSONObject) root.get("data");
        JSONObject custom = (JSONObject) data.get("custom");
        JSONObject args = (JSONObject) custom.get("args");

        assertEquals("{\"outer\":{\"inner\":\"innervalue\"}}", args.toString());
    }

    @Test
    public void contextContainsRootLoggerName() throws JSONException {
        rootLogger.info("test message");

        HttpRequest request = httpRequester.getRequest();
        JSONObject root = new JSONObject(new String(request.getBody()));

        assertEquals(root.getJSONObject("data").get("context"), Logger.ROOT_LOGGER_NAME);
    }

    @Test
    public void contextContainsSubLoggerName() throws JSONException {
        Logger subLogger = (Logger) LoggerFactory.getLogger("some.logger");
        subLogger.addAppender(appender);

        subLogger.warn("test message2");

        HttpRequest request = httpRequester.getRequest();
        JSONObject root = new JSONObject(new String(request.getBody()));

        assertEquals(root.getJSONObject("data").get("context"), "some.logger");
    }
}
