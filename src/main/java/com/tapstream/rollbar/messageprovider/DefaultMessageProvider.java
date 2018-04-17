package com.tapstream.rollbar.messageprovider;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.tapstream.rollbar.Message;
import com.tapstream.rollbar.MessageProvider;

public class DefaultMessageProvider implements MessageProvider {
    @Override
    public Message get(ILoggingEvent event) {
        return new Message(event.getFormattedMessage());
    }
}
