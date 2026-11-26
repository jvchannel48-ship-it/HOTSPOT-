package com.example.internetspeedlimiter;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;

public class LimiterVpnService extends VpnService {
    private ParcelFileDescriptor vpnInterface;
    @Override public void onDestroy() {
        try { if (vpnInterface != null) vpnInterface.close(); } catch (Exception ignored) {}
        vpnInterface = null;
        super.onDestroy();
    }
}
