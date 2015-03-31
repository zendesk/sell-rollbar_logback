package com.tapstream.rollbar.fingerprint;

import static org.assertj.core.api.Assertions.assertThat;

import com.tapstream.rollbar.fingerprinter.CustomFingerprinter;
import com.tapstream.rollbar.fingerprinter.HasFingerprint;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class CustomFingerprinterTest {

    @InjectMocks
    private CustomFingerprinter fingerprinter;
    
    @Test
    public void messageFingerprintIsItsMD5() {
        String f = fingerprinter.fingerprint("msgA", null, null, null);
        
        assertThat(f).isEqualTo(DigestUtils.md5Hex("msgA"));
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void messageFingerprintDependsOnlyOnMessageText() {
        String f = fingerprinter.fingerprint("msgA", null, null, null);
        
        assertThat(fingerprinter.fingerprint("msgA", null, new HashMap(), null)).isEqualTo(f);
        assertThat(fingerprinter.fingerprint("msgA", null, null, "asd")).isEqualTo(f);
        assertThat(fingerprinter.fingerprint("msgA", null, new HashMap() {{put("a","b");}}, "asd")).isEqualTo(f);
        
        assertThat(fingerprinter.fingerprint("msgB", null, null, null)).isNotEqualTo(f);
        assertThat(fingerprinter.fingerprint("msg1", null, null, null)).isNotEqualTo(f);
    }
    
    @Test
    public void numbersAreStrippedFromMessages() {
        String f = fingerprinter.fingerprint("msg1", null, null, null);
        
        assertThat(fingerprinter.fingerprint("msg2", null, null, null)).isEqualTo(f);
        assertThat(fingerprinter.fingerprint("msg22", null, null, null)).isEqualTo(f);
        assertThat(fingerprinter.fingerprint("msg2.2", null, null, null)).isNotEqualTo(f);
        assertThat(fingerprinter.fingerprint("msg2,2", null, null, null)).isNotEqualTo(f);
    }
    
    @Test
    public void whitespacesInMessagesMatter() {
        String f = fingerprinter.fingerprint("msg", null, null, null);
        
        assertThat(fingerprinter.fingerprint("msg 2", null, null, null)).isNotEqualTo(f);
        assertThat(fingerprinter.fingerprint("msg ", null, null, null)).isNotEqualTo(f);
        assertThat(fingerprinter.fingerprint(" msg", null, null, null)).isNotEqualTo(f);
    }
    
    @Test
    public void exceptionOverridesMessage() {
        String m = fingerprinter.fingerprint("msg", null, null, null);
        
        String e1 = fingerprinter.fingerprint("msg", new RuntimeException(), null, null);
        String e2 = fingerprinter.fingerprint("msgother", new RuntimeException(), null, null);
        String e3 = fingerprinter.fingerprint(null, new RuntimeException(), null, null);
        
        assertThat(e1).isNotEqualTo(m);
        assertThat(e2).isNotEqualTo(m);
        assertThat(e3).isNotEqualTo(m);
    }
    
    @Test
    public void sameExceptionsHaveSameFingerprint() {
        String e1 = fingerprinter.fingerprint("msg", new RuntimeException(), null, null);
        String e2 = fingerprinter.fingerprint("msgother", new RuntimeException(), null, null);
        String e3 = fingerprinter.fingerprint(null, new RuntimeException(), null, null);
        assertThat(e1).isEqualTo(e2);
        assertThat(e1).isEqualTo(e3);
        assertThat(e2).isEqualTo(e3); // just in case...
    }
    
    @Test
    public void exceptionMessageIsIgnored() {
        String e1 = fingerprinter.fingerprint(null, new IllegalArgumentException("one"), null, null);
        String e2 = fingerprinter.fingerprint(null, new IllegalArgumentException("two"), null, null);
        
        assertThat(e1).isEqualTo(e2);
    }
    
    @Test
    public void fingerprintDependsOnExceptionClass() {
        String e1 = fingerprinter.fingerprint(null, new RuntimeException(), null, null);
        String e2 = fingerprinter.fingerprint(null, new IllegalArgumentException(), null, null);
        
        assertThat(e1).isNotEqualTo(e2);
    }
    
    @Test
    public void sameExceptionsWithSameCausesHaveSameFingerprint() {
        String e1 = fingerprinter.fingerprint(null, new RuntimeException(new IllegalArgumentException()), null, null);
        String e2 = fingerprinter.fingerprint(null, new RuntimeException(new IllegalArgumentException()), null, null);
        assertThat(e1).isEqualTo(e2);
    }
    
    @Test
    public void fingerprintDependsOnCauseClass() {
        String e1 = fingerprinter.fingerprint(null, new RuntimeException(new RuntimeException()), null, null);
        String e2 = fingerprinter.fingerprint(null, new RuntimeException(new IllegalArgumentException()), null, null);
        
        assertThat(e1).isNotEqualTo(e2);
    }
    
    @Test
    public void exceptionInLambdaIgnoresLambdaInstanceNr() {
        Exception a = new RuntimeException();
        a.setStackTrace(new StackTraceElement[] {new StackTraceElement("protobuf.ProtoTranslatorImpl$$Lambda$68/978649911", "test", "a", 1)});
        Exception b = new RuntimeException();
        b.setStackTrace(new StackTraceElement[] {new StackTraceElement("protobuf.ProtoTranslatorImpl$$Lambda$68/222222222", "test", "a", 1)});
        
        String e1 = fingerprinter.fingerprint(null, a, null, null);
        String e2 = fingerprinter.fingerprint(null, b, null, null);
        
        assertThat(e1).isEqualTo(e2);
    }
    
    @Test
    public void numbersInMethodNamesAreIgnored() {
        Exception a = new RuntimeException();
        a.setStackTrace(new StackTraceElement[] {new StackTraceElement("someclass", "test", "a", 1)});
        Exception b = new RuntimeException();
        b.setStackTrace(new StackTraceElement[] {new StackTraceElement("someclass", "test1", "a", 1)});
        
        String e1 = fingerprinter.fingerprint(null, a, null, null);
        String e2 = fingerprinter.fingerprint(null, b, null, null);
        
        assertThat(e1).isEqualTo(e2);
    }
    
    @Test
    public void exceptionCauseMessageIsIgnored() {
        String e1 = fingerprinter.fingerprint(null, new RuntimeException(new RuntimeException("once")), null, null);
        String e2 = fingerprinter.fingerprint(null, new RuntimeException(new RuntimeException("twice")), null, null);
        
        assertThat(e1).isEqualTo(e2);
    }
    
    private RuntimeException getOther() {
        return new RuntimeException();
    }
    
    @Test
    public void fingerprintDependsOnStacktrace() {
        String e1 = fingerprinter.fingerprint(null, getOther(), null, null);
        String e2 = fingerprinter.fingerprint(null, new RuntimeException(), null, null);
        
        assertThat(e1).isNotEqualTo(e2);
    }
    
    @Test
    public void noMessageOrThrowable() {
        String f = fingerprinter.fingerprint(null, null, null, null);
        
        assertThat(f).isNull();
    }
    
    private static class MyHasFingerprint extends RuntimeException implements HasFingerprint {
        private String fingerprint;
        
        public MyHasFingerprint(String msg, String fingerprint) {
            super(msg);
            this.fingerprint = fingerprint;
        }
        @Override
        public String getFingerprint() {
            return fingerprint;
        }
    }
    
    @Test
    public void fingerprintDependsOnHasFingerprintInterface_different() {
        String e1 = fingerprinter.fingerprint(null, new MyHasFingerprint("msg", "a"), null, null);
        String e2 = fingerprinter.fingerprint(null, new MyHasFingerprint("msg", "b"), null, null);
        
        assertThat(e1).isNotEqualTo(e2);
    }
    
    @Test
    public void fingerprintDependsOnCauseHasFingerprintInterface_different() {
        String e1 = fingerprinter.fingerprint(null, new RuntimeException(new MyHasFingerprint("msg", "a")), null, null);
        String e2 = fingerprinter.fingerprint(null, new RuntimeException(new MyHasFingerprint("msg", "b")), null, null);
        
        assertThat(e1).isNotEqualTo(e2);
    }
    
    @Test
    public void fingerprintDependsOnHasFingerprintInterface_same() {
        String e1 = fingerprinter.fingerprint(null, new MyHasFingerprint("msg", "a"), null, null);
        String e2 = fingerprinter.fingerprint(null, new MyHasFingerprint("msg", "a"), null, null);
        
        assertThat(e1).isEqualTo(e2);
    }
    
    @Test
    public void fingerprintDependsOnHasCauseFingerprintInterface_same() {
        String e1 = fingerprinter.fingerprint(null, new RuntimeException(new MyHasFingerprint("msg", "a")), null, null);
        String e2 = fingerprinter.fingerprint(null, new RuntimeException(new MyHasFingerprint("msg", "a")), null, null);
        
        assertThat(e1).isEqualTo(e2);
    }
}
