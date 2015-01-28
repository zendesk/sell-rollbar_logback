package com.tapstream.rollbar;

import com.tapstream.rollbar.fingerprinter.Fingerprinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotifyBuilder {
    private static final String PERSON_PREFIX = "person.";
    private final String accessToken;
    private final String environment;

    private final JSONObject notifierData;
    private final JSONObject serverData;
    private Fingerprinter fingerprinter;

    public NotifyBuilder(String accessToken, String environment, ServerDataProvider serverDataProvider,
                    NotifierDataProvider notifierDataProvider, Fingerprinter fingerprinter) throws JSONException, RollbarException {
        this.accessToken = accessToken;
        this.environment = environment;
        this.notifierData = notifierDataProvider.getNotifierData();
        this.serverData = serverDataProvider.getServerData();
        this.fingerprinter = fingerprinter;
    }

    private String getValue(String key, Map<String, String> context, String defaultValue) {
        if (context == null) {
            return defaultValue;
        }
        Object value = context.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    public JSONObject build(String level, String message, Throwable throwable, Map<String, String> context, String loggerName) throws JSONException {

        JSONObject payload = new JSONObject();

        // access token
        payload.put("access_token", this.accessToken);
        
        maybeAddFingerprint(message, throwable, context, loggerName, payload);

        // data
        JSONObject data = new JSONObject();

        // general values
        data.put("environment", this.environment);
        data.put("level", level);
        data.put("platform", getValue("platform", context, "java"));
        data.put("framework", getValue("framework", context, "java"));
        data.put("language", "java");
        if (loggerName != null && !loggerName.isEmpty()) {
            data.put("context", loggerName);
        }
        data.put("timestamp", System.currentTimeMillis() / 1000);
        data.put("body", getBody(message, throwable));
        data.put("request", buildRequest(context));

        // Custom data and log message if there's a throwable
        JSONObject customData = buildCustom(context);
        if (throwable != null && message != null) {
            customData.put("log", message);
        }
        
        JSONObject person = buildPerson(context);
        if(person != null){
            data.put("person", person);
        }

        data.put("custom", customData);
        data.put("client", buildClient(context));
        data.put("server", serverData);
        data.put("notifier", notifierData);
        payload.put("data", data);

        return payload;
    }

    private void maybeAddFingerprint(String message, Throwable throwable, Map<String, String> context, String loggerName, JSONObject payload) {
        if(fingerprinter != null) {
            String fingerprint = fingerprinter.fingerprint(message, throwable, context, loggerName);
            if(fingerprint != null) {
                payload.put("fingerprint", fingerprint);
            }
        }
    }
    
    private JSONObject buildPerson(Map<String, String> ctx) {
        JSONObject person = new JSONObject();
        for (Entry<String, String> ctxEntry : ctx.entrySet()) {
            String key = ctxEntry.getKey();
            if (key.startsWith(PERSON_PREFIX)) {
                person.put(stripPrefix(key, PERSON_PREFIX), ctxEntry.getValue());
            }
        }
        if (person.keySet().isEmpty()) {
            return null;
        } else {
            return person;
        }
    }

    private JSONObject buildClient(Map<String, String> ctx){
        JSONObject client = new JSONObject();
        JSONObject javaScript = new JSONObject();
        javaScript.put("browser", ctx.get(RollbarFilter.REQUEST_USER_AGENT));
        client.put("javascript", javaScript);
        return client;
    }
    
    private JSONObject buildCustom(Map<String, String> ctx){
        JSONObject custom = new JSONObject();
        for (Entry<String, String> ctxEntry : ctx.entrySet()){
            String key = ctxEntry.getKey();
            if (!key.startsWith(RollbarFilter.REQUEST_PREFIX) && !key.startsWith(PERSON_PREFIX)){
                custom.put(key, ctxEntry.getValue());
            }
        }
        return custom;
    }
    
    private String stripPrefix(String value, String prefix){
        return value.substring(prefix.length(), value.length());
    }
    
    private JSONObject buildRequest(Map<String, String> ctx){
        JSONObject request = new JSONObject();
        request.put("url", ctx.get(RollbarFilter.REQUEST_URL));
        request.put("query_string", ctx.get(RollbarFilter.REQUEST_QS));
        
        JSONObject headers = new JSONObject();
        JSONObject params = new JSONObject();
        
        for (Entry<String, String> ctxEntry : ctx.entrySet()){
            String key = ctxEntry.getKey();
            if (key.startsWith(RollbarFilter.REQUEST_HEADER_PREFIX)){
                headers.put(stripPrefix(key, RollbarFilter.REQUEST_HEADER_PREFIX), ctxEntry.getValue());
            } else if (key.startsWith(RollbarFilter.REQUEST_PARAM_PREFIX)){
                params.put(stripPrefix(key, RollbarFilter.REQUEST_PARAM_PREFIX), ctxEntry.getValue());
            }
        }
        
        request.put("headers", headers);
        
        String method = ctx.get(RollbarFilter.REQUEST_METHOD);
        if (method != null) {
            request.put("method", method);
            request.put(method, params);
        }
        
        request.put("user_ip", ctx.get(RollbarFilter.REQUEST_REMOTE_ADDR));
        return request;
    }

    private JSONObject getBody(String message, Throwable original) throws JSONException {
        JSONObject body = new JSONObject();

        Throwable throwable = original;

        if (throwable != null) {
            List<JSONObject> traces = new ArrayList<JSONObject>();
            do {
                traces.add(0, createTrace(throwable));
                throwable = throwable.getCause();
            } while (throwable != null);
            
            // TODO consider in the future: if description is present it becomes the label for rollbar item instead of exception's message
            // traces.get(0).getJSONObject("exception").put("description", "Something went wrong while trying to save the user object");

            body.put("trace_chain", new JSONArray(traces));
        }

        // note - can't send both message and exception, rollbar does not accept it
        if (original == null && message != null) {
            JSONObject messageBody = new JSONObject();
            messageBody.put("body", message);
            body.put("message", messageBody);
        }

        return body;
    }

    private JSONObject createTrace(Throwable throwable) throws JSONException {
        JSONObject trace = new JSONObject();

        JSONArray frames = new JSONArray();

        StackTraceElement[] elements = throwable.getStackTrace();
        for (int i = elements.length - 1; i >= 0; --i) {
            StackTraceElement element = elements[i];

            JSONObject frame = new JSONObject();

            frame.put("class_name", element.getClassName());
            frame.put("filename", element.getFileName());
            frame.put("method", element.getMethodName());

            if (element.getLineNumber() > 0) {
                frame.put("lineno", element.getLineNumber());
            }

            frames.put(frame);
        }

        JSONObject exceptionData = new JSONObject();
        exceptionData.put("class", throwable.getClass().getName());
        exceptionData.put("message", throwable.getMessage());

        trace.put("frames", frames);
        trace.put("exception", exceptionData);

        return trace;
    }

}
