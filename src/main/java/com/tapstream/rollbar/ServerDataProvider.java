package com.tapstream.rollbar;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerDataProvider {
    public JSONObject getServerData() throws JSONException, RollbarException {
        InetAddress localhost;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // that's a bit unlikely
            throw new RollbarException("Unknown localhost", e);
        }

        String host = localhost.getHostName();
        String ip = localhost.getHostAddress();

        JSONObject notifier = new JSONObject();
        notifier.put("host", host);
        notifier.put("ip", ip);
        return notifier;
    }
}
