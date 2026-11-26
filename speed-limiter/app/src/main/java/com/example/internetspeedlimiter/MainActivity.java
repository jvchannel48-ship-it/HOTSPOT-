package com.example.internetspeedlimiter;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView text = new TextView(this);
        text.setPadding(32,32,32,32);
        text.setText("Internet Speed Limiter\\n\\nPrototype only: bandwidth shaping and packet forwarding are not implemented yet.");
        setContentView(text);
    }
}
