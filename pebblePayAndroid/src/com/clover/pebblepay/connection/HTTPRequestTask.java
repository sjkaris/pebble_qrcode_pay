package com.clover.pebblepay.connection;

import android.os.AsyncTask;
import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public abstract class HTTPRequestTask extends AsyncTask<String, Void, JSONObject>{

    String urlString;
    
    public abstract InputStream getHttpContent() throws HTTPConnectionException;

    @Override
    protected JSONObject doInBackground(String... params) {
        
        try {
            InputStream is = getHttpContent();

            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer sresponse = new StringBuffer();
            while((line = rd.readLine()) != null) {
                sresponse.append(line);
                sresponse.append('\r');
            }
            rd.close();
            return new JSONObject(sresponse.toString());
        } catch (HTTPConnectionException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

class Pair implements NameValuePair {
    String name, value;

    Pair(String n, String v) {
        name = n;
        value = v;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
