package com.chad;

import android.content.Context;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class AuthSecure {
    private String appname;
    private String ownerid;
    private String version;
    private String url;
    private String secret; // added secret
    private Context context;
    private String sessionid; // Store session ID

    public AuthSecure(String appname, String ownerid, String version, String url, String secret, Context context) {
        this.appname = appname;
        this.ownerid = ownerid;
        this.version = version;
        this.url = url;
        this.secret = secret; // set secret
        this.context = context;
        this.sessionid = null; // Initialize as null
    }

    public void init() throws Exception {
        String initUrl = url +
                "?type=init" +
                "&name=" + URLEncoder.encode(appname, "UTF-8") +
                "&ownerid=" + URLEncoder.encode(ownerid, "UTF-8") +
                "&secret=" + URLEncoder.encode(secret, "UTF-8")+
                "&version=" + URLEncoder.encode(version, "UTF-8") ;


        JSONObject responseJSON = makeApiCall(initUrl);
        if (!responseJSON.getBoolean("success")) {
            throw new Exception(responseJSON.optString("message", "Initialization failed"));
        }
        // Store sessionid from response
        this.sessionid = responseJSON.optString("sessionid", null);
        if (this.sessionid == null || this.sessionid.isEmpty()) {
            throw new Exception("Session ID not provided in init response");
        }
    }

    public void login(String username, String password) throws Exception {
        if (sessionid == null || sessionid.isEmpty()) {
            throw new Exception("Session ID not initialized. Call init first.");
        }

        // ✅ HWID fetch and patch
        String rawHwid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String hwid = rawHwid; // best: no suffix, no slicing

        String loginUrl = url +
                "?type=login" +
                "&username=" + URLEncoder.encode(username, "UTF-8") +
                "&pass=" + URLEncoder.encode(password, "UTF-8") +
                "&hwid=" + URLEncoder.encode(hwid, "UTF-8") +
                "&sessionid=" + URLEncoder.encode(sessionid, "UTF-8") +
                "&name=" + URLEncoder.encode(appname, "UTF-8") +
                "&ownerid=" + URLEncoder.encode(ownerid, "UTF-8");

        JSONObject responseJSON = makeApiCall(loginUrl);

        if (!responseJSON.getBoolean("success")) {
            throw new Exception(responseJSON.optString("message", "Login failed"));
        }

        // ✅ Store AuthSecure instance globally
        AuthSecureInstance.AuthSecure = this;

        // ✅ Read user info
        JSONObject info = responseJSON.getJSONObject("info");
        this.userinfo.username = info.optString("username", "");
        this.userinfo.ip = info.optString("ip", "");
        this.userinfo.hwid = info.optString("hwid", "");

        // ✅ Subscription expiry (inside subscriptions array)
        this.userinfo.expiry = info.getJSONArray("subscriptions")
                .getJSONObject(0)
                .optString("expiry", "");
    }


    public void register(String username, String password, String license) throws Exception {
        if (sessionid == null || sessionid.isEmpty()) {
            throw new Exception("Session ID not initialized. Call init first.");
        }

        String rawHwid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String hwid = rawHwid; // best: no suffix, no slicing


        String registerUrl = url +
                "?type=register" +
                "&username=" + URLEncoder.encode(username, "UTF-8") +
                "&pass=" + URLEncoder.encode(password, "UTF-8") +
                "&license=" + URLEncoder.encode(license, "UTF-8") +
                "&hwid=" + URLEncoder.encode(hwid, "UTF-8") +
                "&sessionid=" + URLEncoder.encode(sessionid, "UTF-8") +
                "&name=" + URLEncoder.encode(appname, "UTF-8") +
                "&ownerid=" + URLEncoder.encode(ownerid, "UTF-8");

        JSONObject responseJSON = makeApiCall(registerUrl);
        if (!responseJSON.getBoolean("success")) {
            throw new Exception(responseJSON.optString("message", "Registration failed"));
        }
    }


    public void license(String license) throws Exception {
        if (sessionid == null || sessionid.isEmpty()) {
            throw new Exception("Session ID not initialized. Call init first.");
        }
        String rawHwid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String hwid = rawHwid; // best: no suffix, no slicing


        String licenseUrl = url +
                "?type=license" +
                "&license=" + URLEncoder.encode(license, "UTF-8") +
                "&hwid=" + URLEncoder.encode(hwid, "UTF-8") +
                "&sessionid=" + URLEncoder.encode(sessionid, "UTF-8") +
                "&name=" + URLEncoder.encode(appname, "UTF-8") +
                "&ownerid=" + URLEncoder.encode(ownerid, "UTF-8") +
                "&secret=" + URLEncoder.encode(secret, "UTF-8");


        JSONObject responseJSON = makeApiCall(licenseUrl);
        if (!responseJSON.getBoolean("success")) {
            throw new Exception(responseJSON.optString("message", "License activation failed"));
        }
    }

    private JSONObject makeApiCall(String apiUrl) throws Exception {
        URL urlObj = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP error code: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();

        return new JSONObject(response.toString());
    }

    // Getter for sessionid (optional, for debugging)
    public String getSessionId() {
        return sessionid;
    }

    public static class UserInfo {
        public String username;
        public String expiry;
        public String ip;
        public String hwid;
    }

    public static UserInfo userinfo = new UserInfo();


}
