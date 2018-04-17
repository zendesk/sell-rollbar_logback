package com.tapstream.rollbar;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.tapstream.rollbar.fingerprinter.DefaultFingerprinter;
import com.tapstream.rollbar.fingerprinter.Fingerprinter;
import com.tapstream.rollbar.messageprovider.DefaultMessageProvider;
import com.tapstream.rollbar.messageprovider.LogstashMessageProvider;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class RollbarAppender extends UnsynchronizedAppenderBase<ILoggingEvent>{
    private NotifyBuilder payloadBuilder;
    
    private URL url;
    private String apiKey;
    private String environment;
    private boolean async = true;
    private IHttpRequester httpRequester = new HttpRequester();
    private Fingerprinter fingerprinter = new DefaultFingerprinter();
    private MessageProvider messageProvider;
    
    public RollbarAppender(){
        try {
            this.url = new URL("https://api.rollbar.com/api/1/item/");
            messageProvider = createMessageProvider();
        } catch (MalformedURLException e) {
            addError("Error initializing url", e);
        }
    }

    public void setHttpRequester(IHttpRequester httpRequester){
        this.httpRequester = httpRequester;
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
        boolean error = false;
        
        if (this.url == null) {
            addError("No url set for the appender named [" + getName() + "].");
            error = true;
        }
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            addError("No apiKey set for the appender named [" + getName() + "].");
            error = true;
        }
        if (this.environment == null || this.environment.isEmpty()) {
            addError("No environment set for the appender named [" + getName() + "].");
            error = true;
        }
   
        try {
            payloadBuilder = new NotifyBuilder(apiKey, environment, new ServerDataProvider(), new NotifierDataProvider(), fingerprinter);
        } catch (JSONException | RollbarException e) {
            addError("Error building NotifyBuilder", e);
            error = true;
        }
        
        if (!error){
            super.start();
        }
        
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        String levelName = event.getLevel().toString().toLowerCase();
        Map<String, String> propertyMap = event.getMDCPropertyMap();
        Message message = messageProvider.get(event);
        
        Throwable throwable = null;
        ThrowableProxy throwableProxy = (ThrowableProxy)event.getThrowableProxy();
        if (throwableProxy != null) {
            throwable = throwableProxy.getThrowable();
        }
        
        String loggerName = event.getLoggerName();
        final JSONObject payload = payloadBuilder.build(levelName, message, throwable, propertyMap, loggerName);
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
            int statusCode = httpRequester.send(request);
            if (statusCode >= 200 && statusCode <= 299){
                // Everything went OK
            } else {
                addError("Non-2xx response from Rollbar: " + statusCode);
            }
            
        } catch (IOException e) {
            addError("Exception sending request to Rollbar", e);
        }
    }

    public void setFingerprinter(Fingerprinter fingerprinter) {
        this.fingerprinter = fingerprinter;
    }

    private MessageProvider createMessageProvider() {
        try {
            Class.forName("net.logstash.logback.argument.StructuredArgument");
            return new LogstashMessageProvider();
        } catch (ClassNotFoundException e) {
            return new DefaultMessageProvider();
        }
    }
}
