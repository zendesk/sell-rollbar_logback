package com.tapstream.rollbar.fingerprinter;

import java.util.Map;

/**
 * Delegates to {@link CustomFingerprinter} only if throwable is provided and it implements 
 * HasFingerprint (or any of its causes implements it). Otherwise returns null indicating 
 * that default Rollbar's algorithm should be used.
 */
public class DefaultFingerprinter implements Fingerprinter {
    private static final int MAX_CAUSES = 100;

    private final CustomFingerprinter customFingerprinter;

    public DefaultFingerprinter() {
        customFingerprinter = new CustomFingerprinter();
    }
    
    // only for tests
    DefaultFingerprinter(CustomFingerprinter fingerprinter){
        this.customFingerprinter = fingerprinter;
    }
    
    @Override
    public String fingerprint(String message, Throwable throwable, Map<String, String> context, String loggerName) {
        if (prividesCustomFingerprint(throwable)) {
            return customFingerprinter.fingerprint(message, throwable, context, loggerName);
        } else {
            return null;
        }
    }

    private boolean prividesCustomFingerprint(Throwable throwable) {
        if (throwable != null) {
            Throwable current = throwable;
            for (int depth = 0; current != null && depth < MAX_CAUSES; depth++) {
                if (current instanceof HasFingerprint) {
                    return true;
                }
                current = current.getCause();
            }
        }
        return false;
    }
}
