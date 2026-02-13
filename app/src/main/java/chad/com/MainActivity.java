package com.chad;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String username = AuthSecureInstance.AuthSecure.userinfo.username;
        String expiry = AuthSecureInstance.AuthSecure.userinfo.expiry;
        String ip = AuthSecureInstance.AuthSecure.userinfo.ip;
        String hwid = AuthSecureInstance.AuthSecure.userinfo.hwid;

        // Root layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#111111"));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(20));

        // Header Title
        TextView header = new TextView(this);
        header.setText("AuthSecure Java Example");
        header.setTextColor(Color.WHITE);
        header.setTextSize(26);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, 0, 0, dp(30));
        root.addView(header);

        // Card Box
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setElevation(dp(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(Color.parseColor("#1A1A1A"));
        card.setBackground(bg);

        // Card Title
        TextView title = new TextView(this);
        title.setText("User Dashboard");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(20));
        card.addView(title);

        // User Info
        card.addView(makeText("👤 Username: " + username));
        card.addView(makeText("🌍 IP Address: " + ip));
        card.addView(makeText("🖥 HWID: " + hwid));
        card.addView(makeText("⏳ Expires In: " + formatExpiry(expiry)));

        // Logout Button
        Button logout = new Button(this);
        logout.setText("Logout");
        logout.setAllCaps(false);
        logout.setTextColor(Color.WHITE);
        logout.setBackgroundColor(Color.parseColor("#FF3B30"));
        logout.setPadding(dp(10), dp(10), dp(10), dp(10));
        logout.setOnClickListener(v -> finish());
        card.addView(logout);

        root.addView(card);
        setContentView(root);
    }

    private String formatExpiry(String expiry) {
        try {
            long ex = Long.parseLong(expiry);
            long now = System.currentTimeMillis() / 1000;
            long diff = ex - now;
            if (diff <= 0) return "Expired";

            long days = diff / 86400;
            long hours = (diff % 86400) / 3600;
            return days + " Days " + hours + " Hours";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private TextView makeText(String s) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(17);
        tv.setPadding(0, dp(8), 0, dp(8));
        return tv;
    }

    private int dp(int x) {
        return (int) (x * getResources().getDisplayMetrics().density);
    }
}
