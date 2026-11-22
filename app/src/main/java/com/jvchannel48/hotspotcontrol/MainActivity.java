package com.jvchannel48.hotspotcontrol;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    private void addCard(LinearLayout parent, String title, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(15), dp(18), dp(15));
        card.setBackground(shape(Color.rgb(28, 39, 58), 18));
        TextView heading = text(title, 17, Color.WHITE, true);
        TextView detail = text(body, 14, Color.rgb(203, 213, 225), false);
        detail.setLineSpacing(dp(3), 1f);
        card.addView(heading);
        card.addView(detail);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(12);
        parent.addView(card, p);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setBackground(shape(Color.rgb(37, 99, 235), 14));
        return b;
    }

    private void openHotspotSettings() {
        try {
            Intent intent = new Intent("android.settings.TETHER_SETTINGS");
            if (intent.resolveActivity(getPackageManager()) == null) {
                intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Buksan ang Settings > Hotspot & tethering.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(11, 18, 32));
        getWindow().setNavigationBarColor(Color.rgb(11, 18, 32));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(11, 18, 32));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(28), dp(20), dp(24));
        scroll.addView(page);

        TextView brand = text("NO ROOT", 13, Color.rgb(96, 165, 250), true);
        brand.setLetterSpacing(0.16f);
        page.addView(brand);
        TextView title = text("HOTSPOT WIFI CONTROL", 25, Color.WHITE, true);
        title.setPadding(0, dp(3), 0, dp(8));
        page.addView(title);
        TextView subtitle = text("Hotspot helper para sa Android phone mo. Walang root at walang bootloader unlock.", 15, Color.rgb(203, 213, 225), false);
        page.addView(subtitle);

        Button settings = button("Buksan ang Hotspot Settings");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(54));
        bp.topMargin = dp(20);
        bp.bottomMargin = dp(16);
        page.addView(settings, bp);
        settings.setOnClickListener(v -> openHotspotSettings());

        addCard(page, "1. Pamahalaan ang nakakonekta",
                "Sa Hotspot Settings ng ilang Android brand, makikita ang connected devices at may option na i-block o alisin ang isang device. Hanapin ang Connected devices / Manage devices.");
        addCard(page, "2. Per-device speed limit",
                "Ang Android ay hindi nagbibigay ng universal public API para magtakda ang ordinaryong app ng Mbps limit sa bawat hotspot client. Lalabas lang ang totoong speed control kung mismong phone manufacturer ang may ganitong feature.");
        addCard(page, "3. Kung walang speed limit sa phone",
                "No-root na alternatibo: gumamit ng compatible router na may bandwidth/QoS control. Ikonekta ang mga user sa router Wi-Fi; ang router ang maglilimita ng speed bawat device. Hindi nito nililimitahan ang mga direktang nakakonekta sa phone hotspot.");
        addCard(page, "Privacy at permissions",
                "Walang hinihinging dangerous permission, walang VPN profile, at walang root access. Hindi namin kinokolekta ang device list o personal data.");

        TextView note = text("STATUS: Basic hotspot settings helper. Ang aktuwal na device list, block at speed limit ay depende sa Android brand/model.", 12, Color.rgb(148, 163, 184), false);
        note.setPadding(dp(2), dp(8), dp(2), dp(8));
        page.addView(note);
        setContentView(scroll);
    }
}
