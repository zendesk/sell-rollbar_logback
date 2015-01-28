package com.tapstream.rollbar.fingerprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.tapstream.rollbar.fingerprinter.CustomFingerprinter;
import com.tapstream.rollbar.fingerprinter.DefaultFingerprinter;
import com.tapstream.rollbar.fingerprinter.HasFingerprint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultFingerprinterTest {
    @InjectMocks
    private DefaultFingerprinter fingerprinter;
    @Mock
    private CustomFingerprinter customFingerprinter;

    @Test
    public void null_whenNoException() {
        assertThat(fingerprinter.fingerprint("asd", null, null, null)).isNull();
    }

    @Test
    public void null_whenRegularException() {
        assertThat(fingerprinter.fingerprint("asd", new RuntimeException(), null, null)).isNull();
    }
    
    @Test
    public void null_whenRegularExceptionWithCause() {
        assertThat(fingerprinter.fingerprint("asd", new RuntimeException(new IllegalArgumentException()), null, null)).isNull();
    }

    private static class CustomException extends RuntimeException implements HasFingerprint {
        private String f;

        public CustomException(String f) {
            this.f = f;
        }

        @Override
        public String getFingerprint() {
            return f;
        }
    }

    @Test
    public void delegates_whenCustomException() {
        CustomException e = new CustomException("x");
        fingerprinter.fingerprint("asd", e, null, null);

        verify(customFingerprinter).fingerprint("asd", e, null, null);
    }
    
    @Test
    public void delegates_whenCustomCause() {
        Exception e = new IllegalArgumentException(new CustomException("x"));
        fingerprinter.fingerprint("asd", e, null, null);

        verify(customFingerprinter).fingerprint("asd", e, null, null);
    }
}
