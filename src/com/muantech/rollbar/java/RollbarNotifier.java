package com.muantech.rollbar.java;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.helpers.LogLog;
import org.json.JSONException;
import org.json.JSONObject;

public class RollbarNotifier {

    public static final int MAX_RETRIES = 5;

    private static NotifyBuilder BUILDER;
    private static URL URL;

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            // thread.setDaemon(true);
            thread.setName("RollbarNotifier-" + new Random().nextInt(100));
            return thread;
        }
    });

    public enum Level {
        DEBUG, INFO, WARNING, ERROR
    }

    public static void init(String urlString, String apiKey, String env) throws JSONException, UnknownHostException {
        URL = getURL(urlString);
        BUILDER = new NotifyBuilder(apiKey, env);
    }

    public static void notify(String message) {
        notify(Level.INFO, message, null);
    }

    public static void notify(String message, Map<String, Object> context) {
        notify(Level.INFO, message, context);
    }

    public static void notify(Level level, String message) {
        notify(level, message, null, null);
    }

    public static void notify(Level level, String message, Map<String, Object> context) {
        notify(level, message, null, context);
    }

    public static void notify(Throwable throwable) {
        notify(Level.ERROR, throwable, null);
    }

    public static void notify(Throwable throwable, Map<String, Object> context) {
        notify(Level.ERROR, throwable, context);
    }

    public static void notify(String message, Throwable throwable) {
        notify(Level.ERROR, message, throwable, null);
    }

    public static void notify(String message, Throwable throwable, Map<String, Object> context) {
        notify(Level.ERROR, message, throwable, context);
    }

    public static void notify(Level level, Throwable throwable) {
        notify(level, null, throwable, null);
    }

    public static void notify(Level level, Throwable throwable, Map<String, Object> context) {
        notify(level, null, throwable, context);
    }

    public static void notify(final Level level, final String message, final Throwable throwable, final Map<String, Object> context) {

        EXECUTOR.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    JSONObject payload = BUILDER.build(level.toString(), message, throwable, context);
                    postJson(payload);
                } catch (Throwable e) {
                    LogLog.error("There was an error notifying the error.", e);
                }
            }

        });

    }

    private static void postJson(JSONObject json) {
        HttpRequest request = new HttpRequest(URL, "POST");

        request.setRequestProperty("Content-Type", "application/json");
        request.setRequestProperty("Accept", "application/json");
        request.setBody(json.toString());

        boolean success = request.execute();
        if (!success && request.getAttemptNumber() < MAX_RETRIES) {
            retryRequest(request);
        }
    }

    private static void retryRequest(final HttpRequest request) {
        EXECUTOR.schedule(new Runnable() {
            @Override
            public void run() {
                request.execute();
            }
        }, request.getAttemptNumber(), TimeUnit.SECONDS);
    }

    private static URL getURL(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            LogLog.error("Error parsing the notifiying URL", e);
            throw new IllegalArgumentException();
        }
        return url;
    }

}
