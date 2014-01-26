package com.muantech.rollbar.java;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.helpers.LogLog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotifyBuilder {

    private static final String NOTIFIER_VERSION = "0.0.1";

    private final String accessToken;
    private final String environment;

    private final JSONObject notifierData;
    private final JSONObject serverData;

    NotifyBuilder(String accessToken, String environment) throws JSONException, UnknownHostException {
        this.accessToken = accessToken;
        this.environment = environment;

        notifierData = getNotifierData();
        serverData = getServerData();
    }

    JSONObject build(String level, String message, Throwable throwable, Map<String, Object> context) throws JSONException {

        JSONObject payload = new JSONObject();

        // access token
        payload.put("access_token", this.accessToken);

        // data
        JSONObject data = new JSONObject();

        // general values
        data.put("environment", this.environment);
        data.put("level", level);
        data.put("platform", getValue("platform", context, "java"));
        data.put("framework", getValue("framework", context, "java"));
        data.put("language", "java");
        data.put("timestamp", System.currentTimeMillis() / 1000);

        // message data
        data.put("body", getBody(message, throwable));

        // request data
        if (context != null) {
            JSONObject requestData = getRequestData(context);
            if (requestData != null && requestData.length() > 0) data.put("request", requestData);
        }

        // custom data
        JSONObject customData = new JSONObject();
        fillCustomData(customData, context);

        // log message
        if (throwable != null && message != null) {
            customData.put("log", message);
        }

        // logs
        if (context != null) {
            JSONArray logsData = getLogsData(context);
            if (logsData != null) customData.put("logs", logsData);
        }

        if (customData.length() > 0) data.put("custom", customData);

        // person data
        if (context != null) {
            JSONObject personData = getPersonData(context);
            if (personData != null) data.put("person", personData);
        }

        // client data
        if (context != null) {
            JSONObject clientData = getClientData(context);
            if (clientData != null) data.put("client", clientData);
        }

        // server data
        data.put("server", serverData);

        // notifier data
        data.put("notifier", notifierData);

        payload.put("data", data);

        return payload;
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

            body.put("trace_chain", new JSONArray(traces));
        }

        if (original == null && message != null) {
            JSONObject messageBody = new JSONObject();
            messageBody.put("body", message);
            body.put("message", messageBody);
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getRequestData(Map<String, Object> context) throws JSONException {

        JSONObject requestData = new JSONObject();

        HttpServletRequest httpRequest = (context.get("request") != null && context.get("request") instanceof HttpServletRequest) ? (HttpServletRequest) context
                .get("request") : null;

        // url: full URL where this event occurred
        String url = getValue("url", context, null);
        if (url == null && httpRequest != null) url = httpRequest.getRequestURI();
        if (url != null) requestData.put("url", url);

        // method: the request method
        String method = getValue("method", context, null);
        if (method == null && httpRequest != null) method = httpRequest.getMethod();
        if (method != null) requestData.put("method", method);

        // headers
        Map<String, String> headers = (Map<String, String>) context.get("headers");
        if (headers == null && httpRequest != null) {
            headers = new HashMap<String, String>();

            Enumeration<String> headerNames = httpRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, httpRequest.getHeader(headerName));
            }
        }
        if (headers != null) {
            JSONObject headersData = new JSONObject();
            for (Entry<String, String> entry : headers.entrySet()) {
                headersData.put(entry.getKey(), entry.getValue());
            }
            if (headersData.length() > 0) requestData.put("headers", headersData);
        }

        // params
        Map<String, String> params = (Map<String, String>) context.get("params");
        if (params == null && httpRequest != null) params = httpRequest.getParameterMap();
        if (params != null) {
            JSONObject paramsData = new JSONObject();
            for (Entry<String, String> entry : params.entrySet()) {
                paramsData.put(entry.getKey(), entry.getValue());
            }
            if (paramsData.length() > 0) {
                String key = method != null ? (method.equalsIgnoreCase("post") ? "POST" : "GET") : "parameters";
                requestData.put(key, paramsData);
            }
        }

        // query string
        String query = (String) context.get("query");
        if (query == null && httpRequest != null) query = httpRequest.getQueryString();
        if (query != null) requestData.put("query_string", query);

        // user ip
        String userIP = (String) context.get("user-ip");
        if (userIP == null && httpRequest != null) userIP = httpRequest.getRemoteAddr();
        if (userIP != null) requestData.put("user_ip", userIP);

        // sessionId
        String sessionId = null;
        Object sessionObj = context.get("session");
        if (sessionObj instanceof HttpSession) {
            sessionId = ((HttpSession) sessionObj).getId();
        } else if (sessionObj instanceof String) {
            sessionId = (String) sessionObj;
        }
        if (sessionId == null && httpRequest != null) {
            HttpSession session = httpRequest.getSession(false);
            if (session != null) sessionId = session.getId();

        }
        if (sessionId != null) requestData.put("session", sessionId);

        // protocol
        String protocol = (String) context.get("protocol");
        if (protocol == null && httpRequest != null) protocol = httpRequest.getProtocol();
        if (protocol != null) requestData.put("protocol", protocol);

        // requestId
        String requestId = (String) context.get("requestId");
        if (requestId != null) requestData.put("id", requestId);

        return requestData;
    }

    @SuppressWarnings("unchecked")
    private JSONObject fillCustomData(JSONObject customData, Map<String, Object> context) throws JSONException {

        for (Entry<String, Object> entry : context.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                customData.put(entry.getKey(), value);
            }
            // HTTPSession attributes
            else if (value instanceof HttpSession) {
                HttpSession session = (HttpSession) value;
                Enumeration<String> attributes = session.getAttributeNames();
                while (attributes.hasMoreElements()) {
                    String nameSession = attributes.nextElement();
                    Object valueSession = session.getAttribute(nameSession);
                    if (valueSession instanceof String) {
                        String str = (String) valueSession;
                        customData.put("session." + nameSession, str);
                    } else if (valueSession instanceof String[]) {
                        String[] array = (String[]) valueSession;
                        customData.put("session." + nameSession, Arrays.asList(array));
                    }
                }

            }
            // HttpServletRequest attributes
            else if (value instanceof HttpServletRequest) {
                HttpServletRequest servletRequest = (HttpServletRequest) value;
                Enumeration<String> attributes = servletRequest.getAttributeNames();
                while (attributes.hasMoreElements()) {
                    String nameRequest = attributes.nextElement();
                    Object valueRequest = servletRequest.getAttribute(nameRequest);
                    if (valueRequest instanceof String) {
                        String str = (String) valueRequest;
                        customData.put("attribute." + nameRequest, str);
                    } else if (valueRequest instanceof String[]) {
                        String[] array = (String[]) valueRequest;
                        customData.put("attribute." + nameRequest, Arrays.asList(array));
                    }
                }

            }
        }

        return customData;
    }

    @SuppressWarnings("unchecked")
    private JSONArray getLogsData(Map<String, Object> context) {
        JSONArray logsData = null;

        List<String> lines = (List<String>) context.get("logs");
        if (lines != null) logsData = new JSONArray(lines);

        return logsData;
    }

    private JSONObject getClientData(Map<String, Object> context) throws JSONException {
        JSONObject clientData = null;

        String browser = getValue("user-agent", context, null);
        if (browser == null) {
            HttpServletRequest request = (HttpServletRequest) context.get("request");
            if (request != null) browser = request.getHeader("User-Agent");
        }

        if (browser != null) {
            clientData = new JSONObject();

            JSONObject javascript = new JSONObject();
            javascript.put("browser", browser);

            clientData.put("javascript", javascript);
        }
        return clientData;
    }

    private JSONObject getPersonData(Map<String, Object> context) throws JSONException {
        JSONObject personData = null;

        String id = getValue("user", context, null);
        if (id != null) {
            personData = new JSONObject();

            personData.put("id", id);
            setIfNotNull("username", personData, context);
            setIfNotNull("email", personData, context);
        }
        return personData;
    }

    private JSONObject getNotifierData() throws JSONException {
        JSONObject notifier = new JSONObject();
        notifier.put("name", "rollbar-java");
        notifier.put("version", NOTIFIER_VERSION);
        return notifier;
    }

    private JSONObject getServerData() throws JSONException, UnknownHostException {

        InetAddress localhost = InetAddress.getLocalHost();

        String host = localhost.getHostName();
        String ip = localhost.getHostAddress();

        JSONObject notifier = new JSONObject();
        notifier.put("host", host);
        notifier.put("ip", ip);
        return notifier;
    }

    private void setIfNotNull(String jsonKey, JSONObject object, Map<String, Object> context) throws JSONException {
        setIfNotNull(jsonKey, object, jsonKey, context);
    }

    private void setIfNotNull(String jsonKey, JSONObject object, String key, Map<String, Object> context) throws JSONException {
        String value = getValue(key, context, null);
        if (value != null) object.put(jsonKey, value);
    }

    private String getValue(String key, Map<String, Object> context, String defaultValue) {
        if (context == null) return defaultValue;
        Object value = context.get(key);
        if (value == null) return defaultValue;
        return value.toString();
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

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);

            throwable.printStackTrace(ps);
            ps.close();
            baos.close();

            trace.put("raw", baos.toString("UTF-8"));
        } catch (Exception e) {
            LogLog.error("Exception printing stack trace.", e);
        }

        JSONObject exceptionData = new JSONObject();
        exceptionData.put("class", throwable.getClass().getName());
        exceptionData.put("message", throwable.getMessage());

        trace.put("frames", frames);
        trace.put("exception", exceptionData);

        return trace;
    }

}
