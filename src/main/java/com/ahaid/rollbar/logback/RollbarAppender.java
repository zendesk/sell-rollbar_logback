package com.ahaid.rollbar.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

public class RollbarAppender extends AppenderBase<ILoggingEvent> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int DEFAULT_LOGS_LIMITS = 100;

    private static boolean init;
    private static LimitedQueue<String> LOG_BUFFER = new LimitedQueue<String>(DEFAULT_LOGS_LIMITS);

    private boolean enabled = true;
    private boolean onlyThrowable = true;
    private boolean logs = true;

    private Level notifyLevel = Level.DEBUG;

    String url = "https://api.rollbar.com/api/1/item/";
    String apiKey;
    String environment;

    @Override
    public void start() {
        setName("RollbarAppender");

        boolean initReady = true;
        if (this.url == null) {
            addError("No url set for the appender named [" + name + "].");
            initReady = false;
        }
        if (this.apiKey == null) {
            addError("No apiKey set for the appender named [" + name + "].");
            initReady = false;
        }
        if (this.environment == null) {
            addError("No environment set for the appender named [" + name + "].");
            initReady = false;
        }
        if (!initReady) {
            return;
        }

        try {
            RollbarNotifier.init(url, apiKey, environment);
        } catch (UnknownHostException e) {
        }
        super.start();
    }

    public void append(ILoggingEvent event) {

        if (!enabled) return;

        try {

            if (!hasToNotify(event.getLevel())) return;

            boolean hasThrowable = thereIsThrowableIn(event);
            if (onlyThrowable && !hasThrowable) return;

            RollbarNotifier.Level level = getMessageLevel(event.getLevel());

            initNotifierIfNeeded();

            final Map<String, Object> context = getContext(event);

            if (hasThrowable) {
                RollbarNotifier.notify(level, event.getMessage(), getThrowable(event), context);
            } else {
                RollbarNotifier.notify(level, event.getMessage(), context);
            }

        } catch (Exception e) {
            logger.error("Error sending error notification! error=" + e.getClass().getName() + " with message=" + e.getMessage());
        }

    }

    private Map<String, Object> getContext(final ILoggingEvent event) {

        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>)(Map<?, ?>)event.getMDCPropertyMap();

        context.put("LOG_BUFFER", new ArrayList<String>(LOG_BUFFER));

        return context;
    }

    public boolean hasToNotify(Level level) {
        return level.isGreaterOrEqual(notifyLevel);
    }

    private synchronized void initNotifierIfNeeded() throws JSONException, UnknownHostException {
        if (init) return;
        RollbarNotifier.init(url, apiKey, environment);
        init = true;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnvironment(final String environment) {
        this.environment = environment;
    }

    public boolean isOnlyThrowable() {
        return onlyThrowable;
    }

    public void setOnlyThrowable(boolean onlyThrowable) {
        this.onlyThrowable = onlyThrowable;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public Level getNotifyLevel() {
        return notifyLevel;
    }

    public void setLevel(String notifyLevel) {
        this.notifyLevel = Level.toLevel(notifyLevel);
    }

    public boolean isLogs() {
        return logs;
    }

    public void setLogs(boolean logs) {
        this.logs = logs;
    }

    public void setLimit(int limit) {
        RollbarAppender.LOG_BUFFER = new LimitedQueue<String>(limit);
    }

    private boolean thereIsThrowableIn(ILoggingEvent loggingEvent) {
        return loggingEvent.getThrowableProxy() != null;
    }

    private Throwable getThrowable(final ILoggingEvent loggingEvent) {
        ThrowableProxy throwableProxy = (ThrowableProxy) loggingEvent.getThrowableProxy();
        Throwable throwable = null;
        if (throwableProxy != null) {
            throwable = throwableProxy.getThrowable();
        }
        return throwable;
    }

    private RollbarNotifier.Level getMessageLevel(Level level){
        return RollbarNotifier.Level.valueOf(level.levelStr.toUpperCase());
    }

    private static class LimitedQueue<E> extends LinkedList<E> {

        private static final long serialVersionUID = 6557339882154255572L;

        private final int limit;

        public LimitedQueue(int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(E o) {
            super.add(o);
            while (size() > limit) {
                super.remove();
            }
            return true;
        }
    }
}