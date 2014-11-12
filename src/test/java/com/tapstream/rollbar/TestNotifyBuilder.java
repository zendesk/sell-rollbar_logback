package com.tapstream.rollbar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestNotifyBuilder {
    @Mock
    ServerDataProvider serverDataProvider;
    @Mock
    NotifierDataProvider notifierDataProvider;
    
    @Before
    public void test() throws Throwable{
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void serverDataIsIncluded() throws JSONException, RollbarException{
        when(serverDataProvider.getServerData()).thenReturn(new JSONObject("{host:abc, ip:10.20.30.40}"));
        
        NotifyBuilder builder = new NotifyBuilder("key", "env", serverDataProvider, notifierDataProvider);
        JSONObject result = builder.build("lvl", "msg", null, new HashMap<String,String>(), "logger.name");
        
        JSONObject data = result.getJSONObject("data");
        assertNotNull(data.getJSONObject("server"));
        JSONObject server = data.getJSONObject("server");
        assertEquals("10.20.30.40", server.get("ip"));
        assertEquals("abc", server.get("host"));
    }
    
    @Test
    public void notifierDataIsIncluded() throws JSONException, RollbarException {
        when(notifierDataProvider.getNotifierData()).thenReturn(new JSONObject("{name:abc, version:'12.0'}"));

        NotifyBuilder builder = new NotifyBuilder("key", "env", serverDataProvider, notifierDataProvider);
        JSONObject result = builder.build("lvl", "msg", null, new HashMap<String, String>(), "logger.name");

        JSONObject data = result.getJSONObject("data");
        assertNotNull(data.getJSONObject("notifier"));
        JSONObject notifier = data.getJSONObject("notifier");
        assertEquals("abc", notifier.get("name"));
        assertEquals("12.0", notifier.get("version"));
    }
    
    @Test
    public void personFieldPresent() throws Exception{
        NotifyBuilder builder = new NotifyBuilder("key", "env", serverDataProvider, notifierDataProvider);
        Map<String,String> ctx = new HashMap<>();
        ctx.put("person.id", "12345");
        ctx.put("person.username", "john");
        ctx.put("person.email", "john@example.org");
        
        JSONObject result = builder.build("lvl", "msg", null, ctx, "logger.name");
        
        assertEquals(true, result.getJSONObject("data").has("person"));
        JSONObject person = result.getJSONObject("data").getJSONObject("person");
        assertEquals("12345", person.get("id"));
        assertEquals("john", person.get("username"));
        assertEquals("john@example.org", person.get("email"));
    }
    
    @Test
    public void personFieldOmitted() throws JSONException, RollbarException{
        NotifyBuilder builder = new NotifyBuilder("key", "env", serverDataProvider, notifierDataProvider);
        Map<String,String> ctx = new HashMap<>();
        
        JSONObject result = builder.build("lvl", "msg", null, ctx, "logger.name");
        
        assertEquals(false, result.getJSONObject("data").has("person"));
    }
}
