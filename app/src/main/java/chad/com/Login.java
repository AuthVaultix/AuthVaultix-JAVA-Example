package com.chad;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;

public class Login extends Activity {
    EditText usernameField, passwordField, licenseField;
    Button loginBtn, activateLicenseBtn, licenseLoginBtn;
    ProgressBar loader;

    private static final String name = "";
    private static final String ownerid = "";
    private static final String secret = "";
    private static final String version = "";
    private static final String url = "https://authvaultix.com/api/1.0/";

    AuthSecure AuthSecure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthSecure = new AuthSecure(name, ownerid, version, url, secret, this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(Color.parseColor("#1a1a1a"));
        box.setBackground(bg);
        box.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---------- TEXT FIELDS ----------
        usernameField = new EditText(this);
        usernameField.setHint("Username");
        style(usernameField);
        box.addView(usernameField);

        passwordField = new EditText(this);
        passwordField.setHint("Password");
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        style(passwordField);
        box.addView(passwordField);

        licenseField = new EditText(this);
        licenseField.setHint("License Key");
        style(licenseField);
        box.addView(licenseField);

        // ---------- BUTTONS ----------
        loginBtn = new Button(this);
        loginBtn.setText("LOGIN");
        styleBtn(loginBtn, "#2196F3");
        box.addView(loginBtn);

        activateLicenseBtn = new Button(this);
        activateLicenseBtn.setText("REGISTER");
        styleBtn(activateLicenseBtn, "#2196F3");
        box.addView(activateLicenseBtn);

        licenseLoginBtn = new Button(this);
        licenseLoginBtn.setText("LICENSE LOGIN");
        styleBtn(licenseLoginBtn, "#2196F3");
        box.addView(licenseLoginBtn);

        loader = new ProgressBar(this);
        loader.setVisibility(ProgressBar.GONE);
        box.addView(loader);

        root.addView(box);
        setContentView(root);

        // CLICK LISTENERS
        loginBtn.setOnClickListener(v -> loginNow());
        activateLicenseBtn.setOnClickListener(v -> registerNow());
        licenseLoginBtn.setOnClickListener(v -> licenseLoginNow());
    }

    private void licenseLoginNow() {
        final String license = licenseField.getText().toString().trim();
        if (license.isEmpty()) {
            toast("Enter License Key");
            return;
        }
        setUiBusy(true);
        new Thread(() -> {
            try {
                AuthSecure.init();
                AuthSecure.license(license);
                new Handler(Looper.getMainLooper()).post(() -> {
                    toast("Logged In ✅");
                    startActivity(new Intent(Login.this, MainActivity.class));
                    finish();
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    toast("Failed ❌ " + e.getMessage());
                    setUiBusy(false);
                });
            }
        }).start();
    }

    private void loginNow() {
        final String user = usernameField.getText().toString().trim();
        final String pass = passwordField.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            toast("Enter Username & Password");
            return;
        }

        setUiBusy(true);

        new Thread(() -> {
            try {
                AuthSecure.init();
                AuthSecure.login(user, pass);
                AuthSecureInstance.AuthSecure = AuthSecure;

                new Handler(Looper.getMainLooper()).post(() -> {
                    toast("Login Successful ✅");
                    startActivity(new Intent(Login.this, MainActivity.class));
                    finish();
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    toast("Login Failed ❌ " + e.getMessage());
                    setUiBusy(false);
                });
            }
        }).start();
    }



    private void registerNow() {
        final String user = usernameField.getText().toString().trim();
        final String pass = passwordField.getText().toString().trim();
        final String license = licenseField.getText().toString().trim();
        if (user.isEmpty() || pass.isEmpty() || license.isEmpty()) {
            toast("Enter Username, Password & License");
            return;
        }
        setUiBusy(true);
        new Thread(() -> {
            try {
                AuthSecure.init();
                AuthSecure.register(user, pass, license);
                new Handler(Looper.getMainLooper()).post(() -> {
                    toast("Registered ✅ Now Login");
                    setUiBusy(false);
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    toast("Register Failed ❌ " + e.getMessage());
                    setUiBusy(false);
                });
            }
        }).start();
    }

    private void setUiBusy(boolean busy) {
        loginBtn.setEnabled(!busy);
        activateLicenseBtn.setEnabled(!busy);
        licenseLoginBtn.setEnabled(!busy);
        loader.setVisibility(busy ? ProgressBar.VISIBLE : ProgressBar.GONE);
    }

    private void style(EditText e) {
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.GRAY);
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        e.setBackgroundColor(Color.parseColor("#2a2a2a"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, dp(10));
        e.setLayoutParams(lp);
    }

    private void styleBtn(Button b, String color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(50));
        bg.setColor(Color.parseColor(color));
        b.setBackground(bg);
        b.setTextColor(Color.WHITE);
        b.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, dp(8));
        b.setLayoutParams(lp);
    }

    private void toast(String t) {
        Toast.makeText(this, t, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
