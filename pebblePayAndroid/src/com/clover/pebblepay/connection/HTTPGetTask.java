package com.clover.pebblepay.connection;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;

public class HTTPGetTask extends HTTPRequestTask {

    public HTTPGetTask(String[] keys, String[] values, String urlString) {
        assert (keys.length == values.length) : "keys and values must have equal length";
        if (keys.length > 0) {
            urlString += "?" + keys[0] + "=" + values[0];
            for (int i = 1; i < keys.length; i++) {
                urlString += "&" + keys[i] + "=" + values[i];
            }
        }
        this.urlString = urlString;
    }

    public HTTPGetTask(String urlString) {
        this.urlString = urlString;
    }

    public InputStream getHttpContent() throws HTTPConnectionException {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(urlString);
            return client.execute(get).getEntity().getContent();
        }
        catch (Exception e) {
            throw new HTTPConnectionException("Error connecting to server", e);
        }
    }
}
