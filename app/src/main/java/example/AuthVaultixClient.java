package example;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AuthVaultixClient {

    private final Context context;
    private final String appName;
    private final String ownerId;
    private final String secret;
    private final String version;
    private final String apiUrl = "https://authvaultix.com/api/1.0/";

    private String responseCollection = "";
    private String lastMessage = "";
    private String lastResponseMessage = "";
    private UserInfo currentUser = null;
    private String sessionId = null;
    private boolean initialized = false;
    private final List<String> userPermissions = new ArrayList<>();
    private String encryptionKey = "";

    public AuthVaultixClient(Context context, String appName, String ownerId, String secret, String version) {
        if (context == null || appName == null || appName.isBlank() ||
            ownerId == null || ownerId.isBlank() ||
            secret == null || secret.isBlank() ||
            version == null || version.isBlank()) {
            crash("Application not setup correctly. AppName, OwnerId, Secret, and Version are required.");
        }
        this.context = context.getApplicationContext();
        this.appName = appName;
        this.ownerId = ownerId;
        this.secret = secret;
        this.version = version;
    }

    public String getResponseCollection() { return responseCollection; }
    public String getLastMessage() { return lastMessage; }
    public String getLastResponseMessage() { return lastResponseMessage; }
    public UserInfo getCurrentUser() { return currentUser; }
    public String getSessionId() { return sessionId; }
    public boolean isInitialized() { return initialized; }

    private void ensureReady() {
        if (!initialized) {
            crash("SDK not initialized. Call client.init() before using any API.");
        }
    }

    public boolean init() {
        responseCollection = "Initialization failed1";
        if (initialized) return true;

        String iv = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        encryptionKey = iv + "-" + secret;

        String hash = getProcessHash();

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "init");
        payload.put("ver", version);
        payload.put("enckey", iv);
        payload.put("hash", hash);
        payload.put("name", appName);
        payload.put("ownerid", ownerId);

        String resp = postRequest(payload, "init");
        if ("Authvaultix_Invalid".equals(resp)) {
            crash("App not found");
        }
        if (resp == null) return false;

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");

        if (success == null || !success) {
            crash(msg != null ? msg : "Initialization Failed");
        }

        sessionId = JsonParser.getJsonString(resp, "sessionid");
        initialized = true;
        return true;
    }

    public boolean login(String username, String password) {
        responseCollection = "";
        ensureReady();

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "login");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("username", username);
        payload.put("pass", password);
        payload.put("hwid", HardwareIdentifier.fetch(context));
        payload.put("os", SystemInfoCollector.getOSVersion());
        payload.put("platform", SystemInfoCollector.getPlatform());
        payload.put("device", SystemInfoCollector.getDeviceType());
        payload.put("architecture", SystemInfoCollector.getArchitecture());
        payload.put("cpu_cores", SystemInfoCollector.getCpuCores());
        payload.put("ram", SystemInfoCollector.getRamGB(context));

        String resp = postRequest(payload, "login");
        if (resp == null) return false;

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        if (success == null || !success) {
            responseCollection = JsonParser.getJsonString(resp, "message");
            if (responseCollection == null) responseCollection = "Login failed";
            return false;
        }

        String infoObj = JsonParser.getJsonObject(resp, "info");
        if (infoObj != null) {
            currentUser = new UserInfo(infoObj);
        }

        List<String> perms = JsonParser.getJsonArrayOfStrings(resp, "permissions");
        userPermissions.clear();
        if (perms != null) {
            userPermissions.addAll(perms);
        }

        String newSess = JsonParser.getJsonString(resp, "sessionid");
        if (newSess != null && !newSess.isBlank()) {
            sessionId = newSess;
        }

        return true;
    }

    public boolean check() {
        responseCollection = "";
        ensureReady();
        if (sessionId == null || sessionId.isBlank()) {
            crash("Session missing");
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "check");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);

        String resp = postRequest(payload, "check");
        if (resp == null) {
            crash("Connection failed");
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null || !success) {
            crash(msg != null ? msg : "Session check failed");
        }

        responseCollection = msg != null ? msg : "OK";
        lastMessage = responseCollection;
        return true;
    }

    public boolean register(String username, String password, String licenseKey, String email) {
        responseCollection = "";
        ensureReady();

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "register");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("username", username);
        payload.put("pass", password);
        payload.put("key", licenseKey);
        payload.put("email", email != null ? email : "");
        payload.put("hwid", HardwareIdentifier.fetch(context));
        payload.put("os", SystemInfoCollector.getOSVersion());
        payload.put("platform", SystemInfoCollector.getPlatform());
        payload.put("device", SystemInfoCollector.getDeviceType());
        payload.put("architecture", SystemInfoCollector.getArchitecture());
        payload.put("cpu_cores", SystemInfoCollector.getCpuCores());
        payload.put("ram", SystemInfoCollector.getRamGB(context));

        String resp = postRequest(payload, "register");
        if (resp == null) return false;

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        if (success == null || !success) {
            responseCollection = JsonParser.getJsonString(resp, "message");
            if (responseCollection == null) responseCollection = "Registration failed";
            return false;
        }

        String infoObj = JsonParser.getJsonObject(resp, "info");
        if (infoObj != null) {
            currentUser = new UserInfo(infoObj);
        }

        List<String> perms = JsonParser.getJsonArrayOfStrings(resp, "permissions");
        userPermissions.clear();
        if (perms != null) {
            userPermissions.addAll(perms);
        }

        String newSess = JsonParser.getJsonString(resp, "sessionid");
        if (newSess != null && !newSess.isBlank()) {
            sessionId = newSess;
        }

        return true;
    }

    public boolean licenseLogin(String licenseKey) {
        responseCollection = "";
        ensureReady();

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "license");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("key", licenseKey);
        payload.put("hwid", HardwareIdentifier.fetch(context));
        payload.put("os", SystemInfoCollector.getOSVersion());
        payload.put("platform", SystemInfoCollector.getPlatform());
        payload.put("device", SystemInfoCollector.getDeviceType());
        payload.put("architecture", SystemInfoCollector.getArchitecture());
        payload.put("cpu_cores", SystemInfoCollector.getCpuCores());
        payload.put("ram", SystemInfoCollector.getRamGB(context));

        String resp = postRequest(payload, "license");
        if (resp == null) return false;

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        if (success == null || !success) {
            responseCollection = JsonParser.getJsonString(resp, "message");
            if (responseCollection == null) responseCollection = "License login failed";
            return false;
        }

        String infoObj = JsonParser.getJsonObject(resp, "info");
        if (infoObj != null) {
            currentUser = new UserInfo(infoObj);
        }

        List<String> perms = JsonParser.getJsonArrayOfStrings(resp, "permissions");
        userPermissions.clear();
        if (perms != null) {
            userPermissions.addAll(perms);
        }

        String newSess = JsonParser.getJsonString(resp, "sessionid");
        if (newSess != null && !newSess.isBlank()) {
            sessionId = newSess;
        }

        return true;
    }

    public boolean log(String message, String[] outServerMessage) {
        ensureReady();
        if (sessionId == null || sessionId.isBlank()) {
            outServerMessage[0] = "Session missing. Please login again.";
            return false;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "log");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("message", message);
        payload.put("pcuser", "Android_Device");

        String resp = postRequest(payload, "log");
        if (resp == null || resp.isBlank()) {
            outServerMessage[0] = "Log request failed (no response).";
            return false;
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null || !success) {
            outServerMessage[0] = msg != null ? msg : "Log failed";
            return false;
        }
        lastMessage = msg != null ? msg : "";
        outServerMessage[0] = lastMessage;
        return true;
    }

    public byte[] download(String fileId, String[] outServerMessage) {
        ensureReady();
        if (sessionId == null || sessionId.isBlank()) {
            outServerMessage[0] = "Session missing. Please login again.";
            return null;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "file");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("fileid", fileId);

        String resp = postRequest(payload, "file");
        if (resp == null || resp.isBlank()) {
            outServerMessage[0] = "Download request failed (no response).";
            return null;
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");

        if (success == null || !success) {
            outServerMessage[0] = msg != null ? msg : "Download failed";
            return null;
        }

        String b64Data = JsonParser.getJsonString(resp, "contents");
        if (b64Data == null || b64Data.isBlank()) {
            outServerMessage[0] = "File content missing";
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(b64Data.trim());
            outServerMessage[0] = msg != null ? msg : "Download successful";
            return bytes;
        } catch (IllegalArgumentException e) {
            outServerMessage[0] = "Invalid file encoding (base64)";
            return null;
        }
    }

    public boolean fetchOnline(List<OnlineUser> outUsers, String[] outServerMessage) {
        ensureReady();
        if (sessionId == null || sessionId.isBlank()) {
            outServerMessage[0] = "Session missing. Please login again.";
            return false;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "fetchonline");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);

        String resp = postRequest(payload, "fetchonline");
        if (resp == null || resp.isBlank()) {
            outServerMessage[0] = "Request failed. Please try again.";
            return false;
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null || !success) {
            outServerMessage[0] = msg != null ? msg : "Failed to fetch online users.";
            return false;
        }

        List<String> rawUsers = JsonParser.getJsonArrayOfObjects(resp, "users");
        outUsers.clear();
        for (String userJson : rawUsers) {
            outUsers.add(new OnlineUser(userJson));
        }

        outServerMessage[0] = msg != null ? msg : "OK";
        return true;
    }

    public boolean ban(String reason, String[] outServerMessage) {
        ensureReady();
        if (sessionId == null || sessionId.isBlank()) {
            outServerMessage[0] = "Session missing. Please login again.";
            return false;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "ban");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("reason", reason != null ? reason : "No reason provided");

        String resp = postRequest(payload, "ban");
        if (resp == null || resp.isBlank()) {
            outServerMessage[0] = "Request failed. Please try again.";
            return false;
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null || !success) {
            outServerMessage[0] = msg != null ? msg : "Ban failed";
            return false;
        }

        outServerMessage[0] = msg != null ? msg : "Banned";
        return true;
    }

    public void logout() {
        ensureReady();
        if (sessionId == null) return;

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "logout");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);

        String resp = postRequest(payload, "logout");
        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null || !success) {
            crash(msg != null ? msg : "Logout Error");
        }

        sessionId = null;
        initialized = false;
        userPermissions.clear();
    }

    public void changeUsername(String newUsername) {
        ensureReady();
        if (newUsername == null || newUsername.isBlank()) {
            crash("New username cannot be empty");
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "changeusername");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("newUsername", newUsername);

        String resp = postRequest(payload, "changeusername");
        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null || !success) {
            crash(msg != null ? msg : "Change username Error");
        }

        sessionId = null;
        initialized = false;
    }

    public boolean checkBlacklist(String[] outServerMessage) {
        ensureReady();
        if (sessionId == null || sessionId.isBlank()) {
            outServerMessage[0] = "Session missing. Please login again.";
            return false;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "checkblacklist");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("hwid", HardwareIdentifier.fetch(context));

        String resp = postRequest(payload, "checkblacklist");
        if (resp == null || resp.isBlank()) {
            outServerMessage[0] = "Request failed. Please try again.";
            return false;
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null || !success) {
            outServerMessage[0] = msg != null ? msg : "Client is blacklisted";
            return false;
        }

        outServerMessage[0] = msg != null ? msg : "Client is not blacklisted";
        return true;
    }

    public boolean forgotPassword(String username, String email) {
        ensureReady();
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "forgot");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("username", username);
        payload.put("email", email);

        String resp = postRequest(payload, "forgot");
        if (resp == null) return false;

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        if (success == null || !success) {
            responseCollection = JsonParser.getJsonString(resp, "message");
            if (responseCollection == null) responseCollection = "Failed";
            return false;
        }
        return true;
    }

    public boolean upgrade(String username, String licenseKey) {
        ensureReady();
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "upgrade");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("username", username);
        payload.put("key", licenseKey);

        String resp = postRequest(payload, "upgrade");
        if (resp == null) return false;

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        if (success == null || !success) {
            responseCollection = JsonParser.getJsonString(resp, "message");
            if (responseCollection == null) responseCollection = "Upgrade Error";
            return false;
        }
        return true;
    }

    public String getGlobalVar(String varKey) {
        responseCollection = "";
        ensureReady();
        if (sessionId == null || sessionId.isBlank()) {
            responseCollection = "Session missing. Please login again.";
            return null;
        }
        if (varKey == null || varKey.isBlank()) {
            responseCollection = "Invalid variable key.";
            return null;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "var");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("varid", varKey);

        String resp = postRequest(payload, "var");
        if (resp == null || resp.isBlank()) {
            responseCollection = "Invalid server response.";
            return null;
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null || !success) {
            responseCollection = msg != null ? msg : "Failed to fetch variable.";
            return null;
        }

        responseCollection = "OK";
        return msg;
    }

    public String getVar(String varName) {
        responseCollection = "";
        ensureReady();
        if (sessionId == null || sessionId.isBlank()) {
            responseCollection = "Session missing. Please login again.";
            return null;
        }
        if (varName == null || varName.isBlank()) {
            responseCollection = "Invalid variable name.";
            return null;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "getvar");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("var", varName);

        String resp = postRequest(payload, "getvar");
        if (resp == null || resp.isBlank()) {
            responseCollection = "Request failed.";
            return null;
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null || !success) {
            responseCollection = msg != null ? msg : "Failed to get variable.";
            return null;
        }

        responseCollection = msg != null ? msg : "OK";
        return JsonParser.getJsonString(resp, "response");
    }

    public boolean setVar(String varName, String value) {
        responseCollection = "";
        ensureReady();
        if (sessionId == null || sessionId.isBlank()) {
            responseCollection = "Session missing. Please login again.";
            return false;
        }
        if (varName == null || varName.isBlank()) {
            responseCollection = "Invalid variable name.";
            return false;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "setvar");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("var", varName);
        payload.put("data", value != null ? value : "");

        String resp = postRequest(payload, "setvar");
        if (resp == null || resp.isBlank()) {
            responseCollection = "Request failed.";
            return false;
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null) {
            responseCollection = "Invalid server response.";
            return false;
        }

        responseCollection = msg != null ? msg : (success ? "OK" : "Failed");
        lastMessage = responseCollection;
        return success;
    }

    public boolean chatSend(String message, String channel, String[] outServerMessage) {
        ensureReady();
        if (sessionId == null || sessionId.isBlank()) {
            outServerMessage[0] = "Session missing. Please login again.";
            return false;
        }
        if (message == null || message.isBlank()) {
            outServerMessage[0] = "Message cannot be empty.";
            return false;
        }
        if (channel == null || channel.isBlank()) {
            outServerMessage[0] = "Invalid channel.";
            return false;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "chatsend");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("message", message);
        payload.put("channel", channel);

        String resp = postRequest(payload, "chatsend");
        if (resp == null || resp.isBlank()) {
            outServerMessage[0] = "Request failed. Please try again.";
            return false;
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        lastResponseMessage = msg != null ? msg : "";

        if (success == null || !success) {
            Integer errCode = JsonParser.getJsonInteger(resp, "code");
            Integer remSec = JsonParser.getJsonInteger(resp, "remaining_seconds");
            if (errCode != null && errCode == 403 && remSec != null && remSec > 0) {
                String mutedTime = JsonParser.getJsonString(resp, "muted_until");
                String mutedHuman = JsonParser.getJsonString(resp, "remaining_human");
                outServerMessage[0] = "Muted till " + mutedTime + " (wait " + mutedHuman + ")";
                lastResponseMessage = outServerMessage[0];
                return false;
            }
            outServerMessage[0] = msg != null ? msg : "Failed to send message.";
            return false;
        }

        outServerMessage[0] = msg != null ? msg : "Message sent.";
        return true;
    }

    public List<ChatMessage> chatFetch(String channel) {
        ensureReady();
        lastResponseMessage = "";
        if (sessionId == null || sessionId.isBlank()) {
            lastResponseMessage = "Session missing. Please login again.";
            return new ArrayList<>();
        }
        if (channel == null || channel.isBlank()) {
            lastResponseMessage = "Invalid channel.";
            return new ArrayList<>();
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", "chatfetch");
        payload.put("name", appName);
        payload.put("ownerid", ownerId);
        payload.put("sessionid", sessionId);
        payload.put("channel", channel);

        String resp = postRequest(payload, "chatfetch");
        if (resp == null || resp.isBlank()) {
            lastResponseMessage = "Request failed. Please try again.";
            return new ArrayList<>();
        }

        Boolean success = JsonParser.getJsonBoolean(resp, "success");
        String msg = JsonParser.getJsonString(resp, "message");
        if (success == null || !success) {
            lastResponseMessage = msg != null ? msg : "Failed to fetch chat messages.";
            return new ArrayList<>();
        }

        List<ChatMessage> list = new ArrayList<>();
        List<String> rawMsgs = JsonParser.getJsonArrayOfObjects(resp, "messages");
        for (String msgJson : rawMsgs) {
            list.add(new ChatMessage(msgJson));
        }

        lastResponseMessage = msg != null ? msg : "OK";
        return list;
    }

    public boolean checkFeaturePermission(String feature) {
        if (feature == null || feature.isEmpty()) return false;
        return userPermissions.contains(feature);
    }

    private String getProcessHash() {
        try {
            String apkPath = context.getPackageCodePath();
            if (apkPath == null || apkPath.isEmpty()) {
                return "android_no_apk_path";
            }
            java.io.File file = new java.io.File(apkPath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "error_calculating_apk_hash";
        }
    }

    private String postRequest(Map<String, String> payload, String actionType) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "AuthVaultixClient/1.0");

            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, String> entry : payload.entrySet()) {
                if (body.length() > 0) body.append("&");
                body.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            byte[] outputInBytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(outputInBytes);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 429) {
                crash("You're connecting too fast, slow down.");
                return null;
            }

            if (responseCode >= 400) {
                crash("Connection failure or network error: HTTP " + responseCode);
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (InputStream is = conn.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }

            String rawResponse = response.toString().trim();
            String signature = conn.getHeaderField("signature");

            if (!verifySignature(rawResponse, signature, actionType)) {
                crash("Signature verification failed. Request tampered");
                return null;
            }
            return rawResponse;
        } catch (Exception ex) {
            crash("Connection failure or network error: " + ex.getMessage());
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {}
            }
        }
    }

    private boolean verifySignature(String payload, String serverSig, String type) {
        if ("log".equals(type) || "file".equals(type)) return true;
        if (serverSig == null || serverSig.isEmpty()) return false;

        String signingKey = "init".equals(type) ? encryptionKey.substring(17, 81) : encryptionKey;
        String localSig = generateHmac(signingKey, payload);
        return cryptographicEquals(localSig, serverSig);
    }

    private String generateHmac(String key, String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hashBytes = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean cryptographicEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

    private void crash(String detail) {
        throw new RuntimeException("AuthVaultix Subsystem Failure: " + detail);
    }

    // --- INNER DTO AND PARSER CLASSES ---

    public static class UserInfo {
        public String username = "";
        public String ip = "";
        public String hwid = "";
        public String createdate = "";
        public String lastlogin = "";
        public List<Subscription> subscriptions = new ArrayList<>();

        public UserInfo(String json) {
            this.username = JsonParser.getJsonString(json, "username");
            this.ip = JsonParser.getJsonString(json, "ip");
            this.hwid = JsonParser.getJsonString(json, "hwid");
            this.createdate = JsonParser.getJsonString(json, "createdate");
            this.lastlogin = JsonParser.getJsonString(json, "lastlogin");

            List<String> subs = JsonParser.getJsonArrayOfObjects(json, "subscriptions");
            for (String subJson : subs) {
                this.subscriptions.add(new Subscription(subJson));
            }
        }

        public String getCreationDateFormatted() {
            return formatUnix(createdate);
        }

        public String getLastLoginFormatted() {
            return formatUnix(lastlogin);
        }

        private String formatUnix(String value) {
            try {
                long ts = Long.parseLong(value);
                LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneId.systemDefault());
                return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"));
            } catch (Exception e) {
                return "Invalid date";
            }
        }
    }

    public static class Subscription {
        public String name = "";
        public String key = "";
        public String expiry = "";
        public long timeLeft = 0;

        public Subscription(String json) {
            this.name = JsonParser.getJsonString(json, "subscription");
            this.key = JsonParser.getJsonString(json, "key");
            this.expiry = JsonParser.getJsonString(json, "expiry");
            Integer tl = JsonParser.getJsonInteger(json, "timeleft");
            this.timeLeft = tl != null ? tl : 0;
        }

        public String getExpiryFormatted() {
            try {
                long ts = Long.parseLong(expiry);
                LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneId.systemDefault());
                return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"));
            } catch (Exception e) {
                return "Invalid date";
            }
        }

        public String getTimeLeftFormatted() {
            if (timeLeft <= 0) return "Expired";
            long days = timeLeft / 86400;
            long hours = (timeLeft % 86400) / 3600;
            long minutes = (timeLeft % 3600) / 60;
            long seconds = timeLeft % 60;
            return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
        }
    }

    public static class OnlineUser {
        public String credential = "";

        public OnlineUser(String json) {
            this.credential = JsonParser.getJsonString(json, "credential");
        }
    }

    public static class ChatMessage {
        public String author = "";
        public String role = "";
        public String message = "";
        public long timestamp = 0;

        public ChatMessage(String json) {
            this.author = JsonParser.getJsonString(json, "author");
            this.role = JsonParser.getJsonString(json, "role");
            this.message = JsonParser.getJsonString(json, "message");
            Integer ts = JsonParser.getJsonInteger(json, "timestamp");
            this.timestamp = ts != null ? ts : 0;
        }

        public String getTimeFormatted() {
            try {
                LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                return dt.format(DateTimeFormatter.ofPattern("hh:mm a"));
            } catch (Exception e) {
                return "--:--";
            }
        }
    }

    private static class JsonParser {
        public static String getJsonString(String json, String key) {
            String pattern = "\"" + key + "\":\\s*\"([^\"]*)\"";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
            return "";
        }

        public static Boolean getJsonBoolean(String json, String key) {
            String pattern = "\"" + key + "\":\\s*(true|false)";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) {
                return Boolean.parseBoolean(m.group(1));
            }
            return null;
        }

        public static java.lang.Integer getJsonInteger(String json, String key) {
            String pattern = "\"" + key + "\":\\s*([0-9]+)";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) {
                return java.lang.Integer.parseInt(m.group(1));
            }
            return null;
        }

        public static String getJsonObject(String json, String key) {
            int keyIdx = json.indexOf("\"" + key + "\":");
            if (keyIdx == -1) return null;
            int startIdx = json.indexOf("{", keyIdx);
            if (startIdx == -1) return null;
            int braceCount = 1;
            int endIdx = startIdx + 1;
            while (braceCount > 0 && endIdx < json.length()) {
                char c = json.charAt(endIdx);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                endIdx++;
            }
            return json.substring(startIdx, endIdx);
        }

        public static List<String> getJsonArrayOfObjects(String json, String key) {
            List<String> list = new ArrayList<>();
            int keyIdx = json.indexOf("\"" + key + "\":");
            if (keyIdx == -1) return list;
            int startIdx = json.indexOf("[", keyIdx);
            if (startIdx == -1) return list;
            int endIdx = json.indexOf("]", startIdx);
            if (endIdx == -1) return list;
            String arrayContent = json.substring(startIdx + 1, endIdx);
            int braceCount = 0;
            int currentStart = -1;
            for (int i = 0; i < arrayContent.length(); i++) {
                char c = arrayContent.charAt(i);
                if (c == '{') {
                    if (braceCount == 0) {
                        currentStart = i;
                    }
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && currentStart != -1) {
                        list.add(arrayContent.substring(currentStart, i + 1));
                    }
                }
            }
            return list;
        }

        public static List<String> getJsonArrayOfStrings(String json, String key) {
            List<String> list = new ArrayList<>();
            int keyIdx = json.indexOf("\"" + key + "\":");
            if (keyIdx == -1) return list;
            int startIdx = json.indexOf("[", keyIdx);
            if (startIdx == -1) return list;
            int endIdx = json.indexOf("]", startIdx);
            if (endIdx == -1) return list;
            String arrayContent = json.substring(startIdx + 1, endIdx);
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"([^\"]*)\"");
            java.util.regex.Matcher m = p.matcher(arrayContent);
            while (m.find()) {
                list.add(m.group(1));
            }
            return list;
        }
    }

    private static class HardwareIdentifier {
        public static String fetch(Context ctx) {
            String deviceId = "UNKNOWN-ANDROID-DEVICE";
            try {
                deviceId = Settings.Secure.getString(
                    ctx.getContentResolver(), 
                    Settings.Secure.ANDROID_ID
                );
            } catch (Exception e) {}

            String raw = String.join("|",
                Build.BOARD,
                Build.BRAND,
                Build.DEVICE,
                Build.HARDWARE,
                Build.MANUFACTURER,
                Build.MODEL,
                Build.PRODUCT,
                deviceId != null ? deviceId : "UNKNOWN"
            );

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    sb.append(String.format("%02X", b));
                }
                String hex = sb.toString();

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < hex.length(); i += 4) {
                    if (i > 0) formatted.append("-");
                    formatted.append(hex.substring(i, Math.min(i + 4, hex.length())));
                }
                return formatted.toString();
            } catch (Exception e) {
                return "ERR-HWID-GEN-ANDROID";
            }
        }
    }

    private static class SystemInfoCollector {
        public static String getOSVersion() {
            return "Android " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")";
        }

        public static String getPlatform() {
            return "android";
        }

        public static String getDeviceType() {
            return "Mobile";
        }

        public static String getArchitecture() {
            if (android.os.Build.SUPPORTED_ABIS != null && android.os.Build.SUPPORTED_ABIS.length > 0) {
                return android.os.Build.SUPPORTED_ABIS[0].toUpperCase();
            }
            return "ARM64";
        }

        public static String getCpuCores() {
            int cores = Runtime.getRuntime().availableProcessors();
            return cores + " Cores / " + cores + " Threads";
        }

        public static String getRamGB(Context context) {
            try {
                android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
                android.app.ActivityManager activityManager = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    activityManager.getMemoryInfo(mi);
                    double totalGb = mi.totalMem / (1024.0 * 1024.0 * 1024.0);
                    return String.valueOf(Math.round(totalGb));
                }
            } catch (Exception e) {
                // fallback
            }
            return "0";
        }
    }
}
