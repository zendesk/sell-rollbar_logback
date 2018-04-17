package com.tapstream.rollbar.messageprovider;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.tapstream.rollbar.Message;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.array;
import static net.logstash.logback.argument.StructuredArguments.entries;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class LogstashMessageProviderTest {
    LogstashMessageProvider p = new LogstashMessageProvider();
    private Logger log;
    private TestAppender appender;

    @Before
    public void setup() {
        log = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        LoggerContext loggerContext = log.getLoggerContext();
        loggerContext.reset();

        appender = new TestAppender();
        appender.setContext(loggerContext);
        appender.start();
        assertTrue(appender.isStarted());
        log.addAppender(appender);
    }

    @Test
    public void mergesKeyValuesIntoSingleObject() throws JSONException {
        log.info("message {}", kv("key", "value"), kv("key2", "value2"), kv("key2", "different"));

        Message message = p.get(appender.event);

        JSONObject args = message.getAdditionalArguments();
        assertThat(args.length()).isEqualTo(2);
        assertThat(args.get("key")).isEqualTo("value");
        assertThat(args.get("key2")).isIn("value2", "different");
    }

    @Test
    public void supportsArrays() throws JSONException {
        log.info("message {}", array("field", 1, 2));

        Message message = p.get(appender.event);

        JSONObject args = message.getAdditionalArguments();
        assertThat(args.length()).isEqualTo(1);
        assertThat(args.get("field")).isEqualTo(new JSONArray("[1,2]"));
    }

    @Test
    public void supportsEntries() throws JSONException {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        Map<String, Integer> b = new HashMap<>();
        b.put("y", 2);
        map.put("x", b);


        log.info("message {}", entries(map));

        Message message = p.get(appender.event);

        JSONObject args = message.getAdditionalArguments();
        JSONObject y = (JSONObject) args.get("x");
        assertThat(y).isNotNull();
        assertThat(y.get("y")).isEqualTo(2);
    }

    @Test
    public void usesUnformattedMessageWithStructuredArguments() {
        log.info("message {}", array("f", 1));

        Message message = p.get(appender.event);

        assertThat(message.getText()).isEqualTo("message {}");
    }

    @Test
    public void usesFormattedMessageIfNoStructuredArgument() {
        log.info("message {}", "xyz");

        Message message = p.get(appender.event);

        assertThat(message.getText()).isEqualTo("message xyz");
    }

    @Test
    public void usesFormattedMessageIfNoArgument() {
        log.info("message");

        Message message = p.get(appender.event);

        assertThat(message.getText()).isEqualTo("message");
    }

    @Test
    public void exceptionIsNotIncluded() {
        log.info("message {}", kv("k", "v"), new RuntimeException());

        Message message = p.get(appender.event);

        assertThat(message.getAdditionalArguments().length()).isEqualTo(1);
        assertThat(message.getAdditionalArguments().has("k")).isTrue();
    }

    static class TestAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
        private ILoggingEvent event;

        @Override
        protected void append(ILoggingEvent event) {
            if (this.event != null) {
                throw new IllegalStateException("already appended an event");
            }
            this.event = event;
        }
    }
}
