package com.tapstream.rollbar.fingerprinter;

import java.util.Map;

public interface Fingerprinter {

    /**
     * @return fingerprint or null if default Rollbar's algorithm should be used for fingerprint generation. 
     */
    String fingerprint(String message, Throwable throwable, Map<String, String> context, String loggerName);

}
