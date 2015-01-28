package com.tapstream.rollbar.sanitize;

public interface HeaderSanitizer {
    /**
     * Returns header value with sensitive information removed.
     * @param headerName
     * @param headerValue
     * @return header value without sensitive information. null if header should be omitted.
     */
    String sanitize(String headerName, String headerValue);
}
