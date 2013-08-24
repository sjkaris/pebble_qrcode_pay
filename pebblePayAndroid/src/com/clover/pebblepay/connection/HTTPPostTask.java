package com.clover.pebblepay.connection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class HTTPPostTask extends HTTPRequestTask {
    
    HttpEntity entity;
    JSONObject JsonObj;
    
    public HTTPPostTask(String[] keys, String[] values, String urlString)
        throws UnsupportedEncodingException{

        assert (keys.length == values.length) : "keys and values must have equal length";
        this.urlString = urlString;
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        for (int i = 0; i < keys.length; i++) {
            pairs.add(new Pair(keys[i], values[i]));
        }
        entity = new UrlEncodedFormEntity(pairs, "UTF-8");
    }

    public HTTPPostTask(HttpEntity entity, String urlString) {
        this.entity = entity;
        this.urlString = urlString;
    }
    
    public HTTPPostTask(JSONObject JsonObj, String urlString)
        throws UnsupportedEncodingException{
        
        this.JsonObj = JsonObj;
        this.urlString = urlString;
        entity = new StringEntity(JsonObj.toString());
        ((StringEntity) entity).setContentType(
                 new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
    }

    public InputStream getHttpContent() throws HTTPConnectionException {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(urlString);

            post.setEntity(entity);

            HttpResponse response = client.execute(post);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity == null) {
                throw new HTTPConnectionException("Null response!");
            }
            return responseEntity.getContent();
        }
        catch (Exception e) {
            throw new HTTPConnectionException("Error connecting to server", e);
        }
    }
}
