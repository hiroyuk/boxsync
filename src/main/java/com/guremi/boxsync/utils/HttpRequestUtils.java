package com.guremi.boxsync.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpRequestUtils {

    public static String get(String uri) throws IOException {
        return execute(new HttpGet(uri));
    }

    public static String post(String uri, Map<String, List<String>> params) throws IOException {
        HttpPost request = new HttpPost(uri);
        List<NameValuePair> p = new ArrayList<>();

        params.forEach((k, l) -> {
            l.forEach(v -> {
                p.add(new BasicNameValuePair(k, v));
            });
        });
        request.setEntity(new UrlEncodedFormEntity(p, StandardCharsets.UTF_8));
        return execute(request);
    }

    private static String execute(HttpRequestBase request) throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        String body = EntityUtils.toString(entity);
        if (response.getStatusLine().getStatusCode() != 200) {
            String errorMsg = String.format("expectd 200 but got %d, with body %s", response.getStatusLine().getStatusCode(), body);
            throw new RuntimeException(errorMsg);
        }
        return body;
    }

    private HttpRequestUtils() {
    }


}
