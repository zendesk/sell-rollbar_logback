package com.tapstream.rollbar.messageprovider;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tapstream.rollbar.Message;
import com.tapstream.rollbar.MessageProvider;
import net.logstash.logback.argument.StructuredArgument;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

/**
 * Extracts unformatted message and structured arguments from LoggingEvent.
 */
public class LogstashMessageProvider implements MessageProvider {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory factory = new JsonFactory();

    private final DefaultMessageProvider defaultProvider = new DefaultMessageProvider();

    @Override
    public Message get(ILoggingEvent event) {
        if (!hasStructuredArgs(event)) {
            return defaultProvider.get(event);
        }

        try {
            JSONObject jsonArgs = argumentsToJsonObject(event.getArgumentArray());

            return new Message(event.getMessage(), jsonArgs);
        } catch (IOException | JSONException | RuntimeException e) {
            JSONObject o = new JSONObject();
            try {
                o.put("error", e.toString());
            } catch (JSONException ignore) {
            }
            return new Message(event.getMessage(), o);
        }
    }

    private JSONObject argumentsToJsonObject(Object[] arguments) throws IOException, JSONException {
        JSONObject jsonArgs = new JSONObject();
        for (int idx = 0; idx < arguments.length; idx++) {
            Object arg = arguments[idx];
            if (arg instanceof StructuredArgument) {
                JSONObject obj = toJsonObject((StructuredArgument) arg);
                merge(jsonArgs, obj);
            } else if (arg != null && arg instanceof Number) {
                jsonArgs.put("arg" + idx, arg);
            } else if (arg != null) {
                jsonArgs.put("arg" + idx, arg.toString());
            } else {
                jsonArgs.put("arg" + idx, null);
            }
        }
        return jsonArgs;
    }

    private boolean hasStructuredArgs(ILoggingEvent event) {
        if (event.getArgumentArray() == null) {
            return false;
        }
        for (Object arg : event.getArgumentArray()) {
            if (arg instanceof StructuredArgument) {
                return true;
            }
        }
        return false;
    }

    private void merge(JSONObject jsonArgs, JSONObject obj) throws JSONException {
        for (Iterator it = obj.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            jsonArgs.put(key, obj.get(key));
        }
    }

    private JSONObject toJsonObject(StructuredArgument arg) throws IOException, JSONException {
        StringWriter w = new StringWriter();
        JsonGenerator gen = factory.createGenerator(w);
        gen.setCodec(mapper);
        gen.writeStartObject();
        arg.writeTo(gen);
        gen.writeEndObject();
        gen.flush();
        return new JSONObject(w.toString());
    }
}
