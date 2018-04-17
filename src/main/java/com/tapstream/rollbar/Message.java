package com.tapstream.rollbar;

import org.json.JSONObject;

public class Message {
    private final String text;
    private final JSONObject additionalArguments;

    public Message(String text) {
        this(text, new JSONObject());
    }

    public Message(String text, JSONObject additionalArguments) {
        this.text = text;
        this.additionalArguments = additionalArguments;
    }

    public String getText() {
        return text;
    }

    public JSONObject getAdditionalArguments() {
        return additionalArguments;
    }
}
