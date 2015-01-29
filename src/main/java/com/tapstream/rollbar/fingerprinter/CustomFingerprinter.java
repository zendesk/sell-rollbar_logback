package com.tapstream.rollbar.fingerprinter;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Calculates fingerprint similar to how Rollbar does that, but supports {@link HasFingerprint} interface.
 * Fingerprint is calculated based on:
 * <ol>
 * <li>when throwable present</li>
 * <ul>
 * <li>throwable's class name</li>
 * <li>throwable's stack trace elements (class name, method name). Numbers are stripped from method names</li>
 * <li>throwable's fingerprint if it implements HasFingerprint</li>
 * <li>throwable.getCause (applies above recursively)</li>
 * </ul>
 * <li>when throwable absent</li>
 * <ul>
 * <li>message with numbers removed</li>
 * </ul>
 * </ol>
 */
public class CustomFingerprinter implements Fingerprinter {
    private static final String NUMBER = "\\d+";
    private static final int MAX_CAUSES = 100;

    @Override
    public String fingerprint(String message, Throwable throwable, Map<String, String> context, String loggerName) {
        if(message == null && throwable == null) {
            return null;
        }
        
        MessageDigest digest = getDigest();
        if (throwable != null) {
            fingerprintThrowable(throwable, digest);
        } else if (message != null) {
            fingerprintMessage(message, digest);
        }
        
        return new String(Hex.encodeHex(digest.digest()));
    }

    protected void fingerprintMessage(String message, MessageDigest digest) {
        digest.update(message.replaceAll(NUMBER, "").getBytes());
    }

    protected void fingerprintThrowable(Throwable throwable, MessageDigest digest) {
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < MAX_CAUSES; depth++) {
            digest.update(current.getClass().getName().getBytes());

            appendHasFingerprint(digest, current);

            appendStacktrace(digest, current);

            current = current.getCause();
        }
    }

    protected void appendStacktrace(MessageDigest digest, Throwable current) {
        if (current.getStackTrace() != null) {
            for (StackTraceElement ste : current.getStackTrace()) {
                appendStacktraceElement(digest, ste);
            }
        }
    }

    protected void appendStacktraceElement(MessageDigest digest, StackTraceElement ste) {
        digest.update(ste.getClassName().getBytes());
        String methodName = ste.getMethodName().replaceAll(NUMBER, "");
        digest.update(methodName.getBytes());
    }

    protected void appendHasFingerprint(MessageDigest digest, Throwable current) {
        if (current instanceof HasFingerprint) {
            String partialFingerprint = ((HasFingerprint) current).getFingerprint();
            if (partialFingerprint != null) {
                digest.update(partialFingerprint.getBytes());
            }
        }
    }

    protected MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to initialize MD5", e);
        }
    }
}
