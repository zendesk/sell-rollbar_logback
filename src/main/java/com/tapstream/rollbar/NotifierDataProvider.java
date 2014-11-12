package com.tapstream.rollbar;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

public class NotifierDataProvider {
    private static final String NOTIFIER_PROPERTIES_RESOURCE = "notifier.properties";

    public JSONObject getNotifierData() throws JSONException, RollbarException {
        try (InputStream input = this.getClass().getResourceAsStream(NOTIFIER_PROPERTIES_RESOURCE)) {
            Properties props = new Properties();
            props.load(input);
            JSONObject notifier = new JSONObject();
            notifier.put("name", props.get("project.artifactId"));
            notifier.put("version", props.get("project.version"));
            return notifier;
        } catch (IOException e) {
            throw new RollbarException("Unable to load " + NOTIFIER_PROPERTIES_RESOURCE, e);
        }
    }
}
