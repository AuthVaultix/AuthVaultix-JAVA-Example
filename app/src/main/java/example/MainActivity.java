package example;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AuthVaultixClient client;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView tvStatus;
    private LinearLayout layoutAuth;
    private LinearLayout layoutDashboard;

    // Auth Views
    private EditText etUsername;
    private EditText etPassword;
    private EditText etLicense;
    private EditText etEmail;
    private Button btnLogin;
    private Button btnRegister;
    private Button btnLicenseLogin;
    private Button btnUpgrade;
    private Button btnForgot;

    // Dashboard Views
    private TextView tvUserData;
    private Button btnCheckSession;
    private Button btnCheckBlacklist;
    private Button btnBan;
    private EditText etLogData;
    private Button btnSendLog;
    private EditText etFileId;
    private Button btnDownload;
    private EditText etGlobalVarKey;
    private Button btnGetGlobalVar;
    private EditText etUserVarKey;
    private EditText etUserVarValue;
    private Button btnGetUserVar;
    private Button btnSetUserVar;
    private TextView tvOnlineUsers;
    private EditText etChatMsg;
    private Button btnSendChat;
    private TextView tvChatMessages;

    private boolean isDashboardLoaded = false;
    private final String chatChannel = "test";
    private final Runnable autoRefreshTask = new Runnable() {
        @Override
        public void run() {
            if (isDashboardLoaded) {
                refreshDashboardData();
            }
            mainHandler.postDelayed(this, 15000); // 15 sec refresh
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        tvStatus = findViewById(R.id.tvStatus);
        layoutAuth = findViewById(R.id.layoutAuth);
        layoutDashboard = findViewById(R.id.layoutDashboard);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etLicense = findViewById(R.id.etLicense);
        etEmail = findViewById(R.id.etEmail);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnLicenseLogin = findViewById(R.id.btnLicenseLogin);
        btnUpgrade = findViewById(R.id.btnUpgrade);
        btnForgot = findViewById(R.id.btnForgot);

        tvUserData = findViewById(R.id.tvUserData);
        btnCheckSession = findViewById(R.id.btnCheckSession);
        btnCheckBlacklist = findViewById(R.id.btnCheckBlacklist);
        btnBan = findViewById(R.id.btnBan);
        etLogData = findViewById(R.id.etLogData);
        btnSendLog = findViewById(R.id.btnSendLog);
        etFileId = findViewById(R.id.etFileId);
        btnDownload = findViewById(R.id.btnDownload);
        etGlobalVarKey = findViewById(R.id.etGlobalVarKey);
        btnGetGlobalVar = findViewById(R.id.btnGetGlobalVar);
        etUserVarKey = findViewById(R.id.etUserVarKey);
        etUserVarValue = findViewById(R.id.etUserVarValue);
        btnGetUserVar = findViewById(R.id.btnGetUserVar);
        btnSetUserVar = findViewById(R.id.btnSetUserVar);
        tvOnlineUsers = findViewById(R.id.tvOnlineUsers);
        etChatMsg = findViewById(R.id.etChatMsg);
        btnSendChat = findViewById(R.id.btnSendChat);
        tvChatMessages = findViewById(R.id.tvChatMessages);

        // Disable input buttons until SDK is initialized
        setButtonsEnabled(false);
        tvStatus.setText("Status: Connecting to servers...");

        // Initialize AuthVaultixClient
        client = new AuthVaultixClient(
            this,
            "",
            "",
            "",
            "1.0"
        );

        // Initialize SDK on background thread
        executorService.execute(() -> {
            boolean success = client.init();
            runOnUiThread(() -> {
                if (success) {
                    tvStatus.setText("Status: Connected successfully!");
                    setButtonsEnabled(true);
                } else {
                    showAlertDialog("Initialization Failed", client.getResponseCollection());
                }
            });
        });

        // Set Click Listeners for Auth
        btnLogin.setOnClickListener(v -> handleLogin());
        btnRegister.setOnClickListener(v -> handleRegister());
        btnLicenseLogin.setOnClickListener(v -> handleLicenseLogin());
        btnUpgrade.setOnClickListener(v -> handleUpgrade());
        btnForgot.setOnClickListener(v -> handleForgot());

        // Set Click Listeners for Dashboard
        btnCheckSession.setOnClickListener(v -> handleCheckSession());
        btnCheckBlacklist.setOnClickListener(v -> handleCheckBlacklist());
        btnBan.setOnClickListener(v -> handleBan());
        btnSendLog.setOnClickListener(v -> handleSendLog());
        btnDownload.setOnClickListener(v -> handleDownloadFile());
        btnGetGlobalVar.setOnClickListener(v -> handleGetGlobalVar());
        btnGetUserVar.setOnClickListener(v -> handleGetUserVar());
        btnSetUserVar.setOnClickListener(v -> handleSetUserVar());
        btnSendChat.setOnClickListener(v -> handleSendChat());

        // Start Auto Refresh task
        mainHandler.postDelayed(autoRefreshTask, 15000);
    }

    private void showAlertDialog(String title, String message) {
        runOnUiThread(() -> {
            new android.app.AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(message != null && !message.isEmpty() ? message : "An error occurred.")
                .setPositiveButton("OK", null)
                .show();
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        btnLogin.setEnabled(enabled);
        btnRegister.setEnabled(enabled);
        btnLicenseLogin.setEnabled(enabled);
        btnUpgrade.setEnabled(enabled);
        btnForgot.setEnabled(enabled);
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlertDialog("Error", "Credentials cannot be empty");
            return;
        }

        tvStatus.setText("Status: Authenticating...");
        setButtonsEnabled(false);

        executorService.execute(() -> {
            boolean success = client.login(username, password);
            runOnUiThread(() -> {
                setButtonsEnabled(true);
                if (success) {
                    tvStatus.setText("Status: Welcome, " + username);
                    loadDashboard();
                } else {
                    tvStatus.setText("Status: Login Failed");
                    showAlertDialog("Login Failed", client.getResponseCollection());
                }
            });
        });
    }

    private void handleRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String license = etLicense.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || license.isEmpty()) {
            showAlertDialog("Error", "Username, Password and License are required");
            return;
        }

        tvStatus.setText("Status: Registering...");
        setButtonsEnabled(false);

        executorService.execute(() -> {
            boolean success = client.register(username, password, license, email);
            runOnUiThread(() -> {
                setButtonsEnabled(true);
                if (success) {
                    tvStatus.setText("Status: Welcome, " + username);
                    loadDashboard();
                } else {
                    tvStatus.setText("Status: Registration Failed");
                    showAlertDialog("Registration Failed", client.getResponseCollection());
                }
            });
        });
    }

    private void handleLicenseLogin() {
        String license = etLicense.getText().toString().trim();

        if (license.isEmpty()) {
            showAlertDialog("Error", "License Key cannot be empty");
            return;
        }

        tvStatus.setText("Status: Verifying license...");
        setButtonsEnabled(false);

        executorService.execute(() -> {
            boolean success = client.licenseLogin(license);
            runOnUiThread(() -> {
                setButtonsEnabled(true);
                if (success) {
                    tvStatus.setText("Status: License verified successfully!");
                    loadDashboard();
                } else {
                    tvStatus.setText("Status: License Verification Failed");
                    showAlertDialog("License Login Failed", client.getResponseCollection());
                }
            });
        });
    }

    private void handleUpgrade() {
        String username = etUsername.getText().toString().trim();
        String license = etLicense.getText().toString().trim();

        if (username.isEmpty() || license.isEmpty()) {
            showAlertDialog("Error", "Username and License Key are required for upgrade");
            return;
        }

        tvStatus.setText("Status: Upgrading...");
        setButtonsEnabled(false);

        executorService.execute(() -> {
            boolean success = client.upgrade(username, license);
            runOnUiThread(() -> {
                setButtonsEnabled(true);
                if (success) {
                    tvStatus.setText("Status: Upgrade successful!");
                    showAlertDialog("Success", "Upgrade successful!");
                } else {
                    tvStatus.setText("Status: Upgrade Failed");
                    showAlertDialog("Upgrade Failed", client.getResponseCollection());
                }
            });
        });
    }

    private void handleForgot() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty()) {
            showAlertDialog("Error", "Username and Email are required to reset password");
            return;
        }

        tvStatus.setText("Status: Triggering reset...");
        setButtonsEnabled(false);

        executorService.execute(() -> {
            boolean success = client.forgotPassword(username, email);
            runOnUiThread(() -> {
                setButtonsEnabled(true);
                if (success) {
                    tvStatus.setText("Status: Reset link sent!");
                    showAlertDialog("Success", "Reset email sent successfully");
                } else {
                    tvStatus.setText("Status: Reset Failed");
                    showAlertDialog("Reset Failed", client.getResponseCollection());
                }
            });
        });
    }

    private void loadDashboard() {
        runOnUiThread(() -> {
            layoutAuth.setVisibility(View.GONE);
            layoutDashboard.setVisibility(View.VISIBLE);
            isDashboardLoaded = true;

            AuthVaultixClient.UserInfo user = client.getCurrentUser();
            if (user != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Username: ").append(user.username).append("\n");
                if (!user.subscriptions.isEmpty()) {
                    AuthVaultixClient.Subscription sub = user.subscriptions.get(0);
                    sb.append("License: ").append(sub.key).append("\n");
                    sb.append("Expires: ").append(sub.getExpiryFormatted()).append("\n");
                    sb.append("Subscription: ").append(sub.name).append("\n");
                    sb.append("Time Left: ").append(sub.getTimeLeftFormatted()).append("\n");
                }
                sb.append("IP: ").append(user.ip).append("\n");
                sb.append("HWID: ").append(user.hwid).append("\n");
                sb.append("Creation Date: ").append(user.getCreationDateFormatted()).append("\n");
                sb.append("Last Login: ").append(user.getLastLoginFormatted()).append("\n");

                // Feature permissions
                sb.append("VIP Feature: ").append(client.checkFeaturePermission("VIP") ? "Access Granted" : "Access Denied").append("\n");
                sb.append("Premium Feature: ").append(client.checkFeaturePermission("premium") ? "Access Granted" : "Access Denied").append("\n");
                sb.append("ESP Feature: ").append(client.checkFeaturePermission("ESP") ? "Access Granted" : "Access Denied");

                tvUserData.setText(sb.toString());
            }

            // Immediately load chat history & online users
            refreshDashboardData();
        });
    }

    private void refreshDashboardData() {
        executorService.execute(() -> {
            // 1. Fetch Online Users
            List<AuthVaultixClient.OnlineUser> onlineUsers = new ArrayList<>();
            String[] outMsg = new String[1];
            boolean usersOk = client.fetchOnline(onlineUsers, outMsg);

            // 2. Fetch Chat History
            List<AuthVaultixClient.ChatMessage> chatMsgs = client.chatFetch(chatChannel);

            runOnUiThread(() -> {
                // Render Online Users
                if (usersOk) {
                    StringBuilder sb = new StringBuilder();
                    for (AuthVaultixClient.OnlineUser u : onlineUsers) {
                        sb.append("â€¢ ").append(u.credential).append("\n");
                    }
                    tvOnlineUsers.setText(sb.toString().trim());
                }

                // Render Chat Messages
                if (chatMsgs != null) {
                    StringBuilder sb = new StringBuilder();
                    for (AuthVaultixClient.ChatMessage msg : chatMsgs) {
                        sb.append("[").append(msg.author).append(" (").append(msg.role).append(")]: ")
                          .append(msg.message).append("\n");
                    }
                    tvChatMessages.setText(sb.toString().trim());
                }
            });
        });
    }

    private void handleCheckSession() {
        executorService.execute(() -> {
            boolean active = client.check();
            runOnUiThread(() -> {
                showAlertDialog("Session Status", active ? "Session is active and valid!" : "Session is invalid!");
            });
        });
    }

    private void handleCheckBlacklist() {
        executorService.execute(() -> {
            String[] outMsg = new String[1];
            boolean isBlacklisted = !client.checkBlacklist(outMsg);
            runOnUiThread(() -> {
                showAlertDialog("Blacklist Check", isBlacklisted ? "User is blacklisted: " + outMsg[0] : "User is not blacklisted.");
            });
        });
    }

    private void handleBan() {
        executorService.execute(() -> {
            String[] outMsg = new String[1];
            boolean success = client.ban("Cheating detected", outMsg);
            runOnUiThread(() -> {
                if (success) {
                    showAlertDialog("Ban Enforced", "You have been banned. App will exit.");
                    finishAffinity();
                    System.exit(0);
                } else {
                    showAlertDialog("Ban Failed", outMsg[0]);
                }
            });
        });
    }

    private void handleSendLog() {
        String logText = etLogData.getText().toString().trim();
        if (logText.isEmpty()) {
            showAlertDialog("Error", "Log text cannot be empty");
            return;
        }

        executorService.execute(() -> {
            String[] outMsg = new String[1];
            boolean success = client.log(logText, outMsg);
            runOnUiThread(() -> {
                showAlertDialog("Send Log", success ? "Log sent successfully: " + outMsg[0] : "Log send failed: " + outMsg[0]);
                if (success) etLogData.setText("");
            });
        });
    }

    private void handleDownloadFile() {
        String fileId = etFileId.getText().toString().trim();
        if (fileId.isEmpty()) {
            showAlertDialog("Error", "File ID cannot be empty");
            return;
        }

        executorService.execute(() -> {
            String[] outMsg = new String[1];
            byte[] fileBytes = client.download(fileId, outMsg);
            runOnUiThread(() -> {
                if (fileBytes != null && fileBytes.length > 0) {
                    try {
                        File downloadFolder = new File(getExternalFilesDir(null), "downloads");
                        if (!downloadFolder.exists()) downloadFolder.mkdirs();
                        File downloadedFile = new File(downloadFolder, fileId + ".bin");
                        try (FileOutputStream fos = new FileOutputStream(downloadedFile)) {
                            fos.write(fileBytes);
                        }
                        showAlertDialog("Success", "Downloaded " + fileBytes.length + " bytes successfully to: " + downloadedFile.getAbsolutePath());
                    } catch (Exception ex) {
                        showAlertDialog("Error saving file", ex.getMessage());
                    }
                } else {
                    showAlertDialog("Download Failed", outMsg[0]);
                }
            });
        });
    }

    private void handleGetGlobalVar() {
        String varKey = etGlobalVarKey.getText().toString().trim();
        if (varKey.isEmpty()) {
            showAlertDialog("Error", "Global variable key cannot be empty");
            return;
        }

        executorService.execute(() -> {
            String val = client.getGlobalVar(varKey);
            runOnUiThread(() -> {
                if (val != null) {
                    showAlertDialog("Global Var", "Global var value: " + val);
                } else {
                    showAlertDialog("Fetch Failed", client.getResponseCollection());
                }
            });
        });
    }

    private void handleGetUserVar() {
        String varKey = etUserVarKey.getText().toString().trim();
        if (varKey.isEmpty()) {
            showAlertDialog("Error", "User variable key cannot be empty");
            return;
        }

        executorService.execute(() -> {
            String val = client.getVar(varKey);
            runOnUiThread(() -> {
                if (val != null) {
                    showAlertDialog("User Var", "User var value: " + val);
                } else {
                    showAlertDialog("Fetch Failed", client.getResponseCollection());
                }
            });
        });
    }

    private void handleSetUserVar() {
        String varKey = etUserVarKey.getText().toString().trim();
        String varVal = etUserVarValue.getText().toString().trim();
        if (varKey.isEmpty() || varVal.isEmpty()) {
            showAlertDialog("Error", "User variable key and value cannot be empty");
            return;
        }

        executorService.execute(() -> {
            boolean success = client.setVar(varKey, varVal);
            runOnUiThread(() -> {
                showAlertDialog("Set User Var", success ? "Successfully set variable!" : "Set failed: " + client.getResponseCollection());
            });
        });
    }

    private void handleSendChat() {
        String chatText = etChatMsg.getText().toString().trim();
        if (chatText.isEmpty()) {
            showAlertDialog("Error", "Chat text cannot be empty");
            return;
        }

        executorService.execute(() -> {
            String[] outMsg = new String[1];
            boolean success = client.chatSend(chatText, chatChannel, outMsg);
            runOnUiThread(() -> {
                if (success) {
                    etChatMsg.setText("");
                    refreshDashboardData();
                } else {
                    showAlertDialog("Chat Send Failed", outMsg[0]);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacks(autoRefreshTask);
        executorService.shutdown();
    }
}
