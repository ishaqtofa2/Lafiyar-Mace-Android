package com.maganimata.app;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Api {
    public interface Callback { void ok(String body); void err(Exception e); }

    private static HttpURLConnection open(String path, String method, String token) throws Exception {
        URL u;
        if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
            u = new URL(path);
        } else if (path != null && path.startsWith("../")) {
            String base = Config.API_BASE_URL == null ? "" : Config.API_BASE_URL.trim();
            if (!base.endsWith("/")) base += "/";
            u = new URL(new URL(base), path);
        } else {
            String cleanPath = path == null ? "" : path.replaceFirst("^/+", "");
            String base = Config.API_BASE_URL == null ? "" : Config.API_BASE_URL.trim();
            if (!base.endsWith("/")) base += "/";
            u = new URL(base + cleanPath);
        }

        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setInstanceFollowRedirects(true);
        boolean isAiRequest = path != null && path.contains("ask_ai");
        c.setConnectTimeout(15000);
        c.setReadTimeout(isAiRequest ? 55000 : 25000);
        c.setRequestMethod(method);
        c.setRequestProperty("Accept", "application/json, text/plain, */*");
        c.setRequestProperty("User-Agent", "LafiyarMace/1.1 Android");
        c.setRequestProperty("Cache-Control", "no-cache");
        if(token != null && !token.isEmpty()) {
            c.setRequestProperty("Authorization", "Bearer " + token);
        }
        if(method.equals("POST")) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        }
        return c;
    }

    public static void get(String path, Callback cb){ get(path, "", cb); }

    public static void get(String path, String token, Callback cb){
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = open(path, "GET", token);
                finish(c, cb);
            } catch(Exception e) {
                cb.err(e);
            } finally {
                if(c != null) c.disconnect();
            }
        }, "api-get").start();
    }

    public static void post(String path, Map<String,String> values, String token, Callback cb){
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = open(path, "POST", token);
                StringBuilder body = new StringBuilder();
                if(values != null) {
                    for(Map.Entry<String,String> e : values.entrySet()) {
                        if(body.length() > 0) body.append('&');
                        body.append(URLEncoder.encode(e.getKey(), "UTF-8"));
                        body.append('=');
                        body.append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), "UTF-8"));
                    }
                }
                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                c.setFixedLengthStreamingMode(data.length);
                try(OutputStream os = c.getOutputStream()) {
                    os.write(data);
                    os.flush();
                }
                finish(c, cb);
            } catch(Exception e) {
                cb.err(e);
            } finally {
                if(c != null) c.disconnect();
            }
        }, "api-post").start();
    }

    private static void finish(HttpURLConnection c, Callback cb) throws Exception {
        int code = c.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        String response = "";

        if(is != null) {
            try(BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder s = new StringBuilder();
                String line;
                while((line = r.readLine()) != null) s.append(line).append('\n');
                response = s.toString().trim();
            }
        }

        if(code >= 200 && code < 300) {
            cb.ok(response);
            return;
        }

        String detail = response;
        if(detail.length() > 500) detail = detail.substring(0, 500) + "…";
        throw new IOException("HTTP " + code + (detail.isEmpty() ? "" : ": " + detail));
    }

    public static String absoluteImageUrl(String raw){
        if(raw == null) return "";
        String value = raw.trim();
        if(value.isEmpty()) return "";
        if(value.startsWith("http://") || value.startsWith("https://")) return value;
        while(value.startsWith("../")) value = value.substring(3);
        while(value.startsWith("./")) value = value.substring(2);
        if(value.startsWith("/magani/")) return "https://gyaranaure.com.ng" + value;
        if(value.startsWith("/")) return "https://gyaranaure.com.ng" + value;
        if(value.startsWith("magani/")) return "https://gyaranaure.com.ng/" + value;
        return Config.WEB_BASE_URL + value;
    }
}
