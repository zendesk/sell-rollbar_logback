package com.tapstream.rollbar;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Extract Message from LoggingEvent.
 * <p>
 * Message contains text and (optionally) extra arguments already serialized to JSONObject. This interface allows
 * to chose implementation based on what types are on the classpath and in the effect to support StructuredArguments
 * from logback-logstash-encoder.
 */
public interface MessageProvider {
    Message get(ILoggingEvent event);
}
