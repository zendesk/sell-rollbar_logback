package com.tapstream.rollbar.sanitize;


public class NoOpHeaderSanitizer implements HeaderSanitizer {

    @Override
    public String sanitize(String headerName, String headerValue) {
        return headerValue;
    }

}
