package com.ahaid.rollbar.logback;

import java.net.UnknownHostException;
import java.util.Map;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;

public class RollbarAppender extends AppenderBase<ILoggingEvent> {

    String url;
    String apiKey;
    String environment;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

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

        // output the events as formatted by our layout
        Throwable throwable = ((ThrowableProxy) event.getThrowableProxy()).getThrowable();

        RollbarNotifier.notify(event.getLevel().levelStr, throwable);

    }
}