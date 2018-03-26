package com.tapstream.rollbar.manual;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.status.OnConsoleStatusListener;

import com.tapstream.rollbar.RollbarAppender;
import com.tapstream.rollbar.fingerprinter.HasFingerprint;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ignored, should not run automatically
@Ignore
public class ManualTest {
    private static final Logger log = LoggerFactory.getLogger(ManualTest.class);
    
    private static final String API_KEY = "..."; // MUST FILL HERE
    private static ResponseCapturingHttpRequestor requestor;
    
    @BeforeClass
    public static void beforeClass() {
            requestor = new ResponseCapturingHttpRequestor();
            // OnConsoleStatusListener.addNewInstanceToContext((Context) LoggerFactory.getILoggerFactory()); // FIXME?
            ch.qos.logback.classic.Logger rootLogger = getRootLogger();
            
            RollbarAppender appender = new RollbarAppender();
            appender.setContext(rootLogger.getLoggerContext());
            appender.setName("rollbar-appender");
            appender.setApiKey(API_KEY);
            appender.setAsync(false);
            appender.setEnvironment("development");
            appender.setHttpRequester(requestor);
            
            final ThresholdFilter filter = new ThresholdFilter();
            filter.setLevel("WARN");
            filter.start();
            appender.addFilter(filter);
            
            rootLogger.addAppender(appender);
            appender.start();
    }
    
    static class MyException2 extends RuntimeException {
        public MyException2() {
            super();
        }
        public MyException2(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
        public MyException2(String message, Throwable cause) {
            super(message, cause);
        }
        public MyException2(String message) {
            super(message);
        }
        public MyException2(Throwable cause) {
            super(cause);
        }
    }
    
    static class MyException3 extends RuntimeException implements HasFingerprint {
        public MyException3(String message) {
            super(message);
        }
        @Override
        public String getFingerprint() {
            return getMessage();
        }
    }
    
    @Test
    public void testMessageGeneration() throws Exception {
        log.error("test03", new MyException2("test3"));
        log.info("response: " + requestor.getLastResponse());
    }
    
    @Test
    public void testAggregation() {
        log.error("test03", new MyException2("test3"));
        log.info("response: " + requestor.getLastResponse());
        
        log.error("test04", new MyException2("test4"));
        log.info("response: " + requestor.getLastResponse());
        
        log.error("test05", new MyException3("test5"));
        log.info("response: " + requestor.getLastResponse());
        log.error("test06", new MyException3("test6"));
        log.info("response: " + requestor.getLastResponse());
    }
    
    private static ch.qos.logback.classic.Logger getRootLogger() {
        LoggerContext loggerCtx = (LoggerContext) LoggerFactory.getILoggerFactory();
        return loggerCtx.getLogger(Logger.ROOT_LOGGER_NAME);
    }
}
