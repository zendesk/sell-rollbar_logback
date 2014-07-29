package com.ahaid.rollbar.logback;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class RollbarAppender extends UnsynchronizedAppenderBase<ILoggingEvent>{    
    
    private NotifyBuilder payloadBuilder;
    
    private URL url;
    private String apiKey;
    private String environment;
    private boolean async = true;
    
    RollbarAppender(){
        try {
            this.url = new URL("https://api.rollbar.com/api/1/item/");
        } catch (MalformedURLException e) {
            addError("Error initializing url", e);
        }
    }
 
    public void setUrl(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            addError("Error setting url", e);
        }
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public void setAsync(boolean async){
        this.async = async;
    }

    @Override
    public void start() {
        if (this.url == null) {
            addError("No url set for the appender named [" + getName() + "].");
        }
        if (this.apiKey == null) {
            addError("No apiKey set for the appender named [" + getName() + "].");
        }
        if (this.environment == null) {
            addError("No environment set for the appender named [" + getName() + "].");
        }
        
        try {
            payloadBuilder = new NotifyBuilder(apiKey, environment);
        } catch (JSONException | UnknownHostException e) {
            addError("Error building NotifyBuilder", e);
        }
        
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        String levelName = event.getLevel().toString().toLowerCase();
        String message = event.getMessage();
        Map<String, Object> propertyMap = (Map)event.getMDCPropertyMap();
        
        Throwable throwable = null;
        ThrowableProxy throwableProxy = (ThrowableProxy)event.getThrowableProxy();
        if (throwableProxy != null)
            throwable = throwableProxy.getThrowable();
        
        final JSONObject payload = payloadBuilder.build(levelName, message, throwable, propertyMap);
        final HttpRequest request = new HttpRequest(url, "POST");
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");
        request.setBody(payload.toString());
        
        if (async){
            getContext().getExecutorService().submit(new Runnable(){
                @Override
                public void run() {
                    sendRequest(request);
                }
            });
        } else {
            sendRequest(request);
        }
        
        
    }
    
    private void sendRequest(HttpRequest request){
        try {
            int statusCode = request.execute();
            if (statusCode >= 200 && statusCode < 299){
                // Everything went OK
            } else {
                addError("Non-2xx response from Rollbar: " + statusCode);
            }
            
        } catch (IOException e) {
            addError("Exception sending request to Rollbar", e);
        }
    }

}
