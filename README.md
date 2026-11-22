# NO ROOT HOTSPOT WIFI CONTROL

Native Android starter app for managing hotspot settings without root.

## Build
The GitHub Actions workflow builds a debug APK on every push to `main`, and can also be started manually from **Actions → Android APK → Run workflow**. Download the APK from the workflow run's Artifacts section.

## Current capabilities
- Opens the Android system Hotspot/Tethering settings.
- Gives clear guidance for connected-device management where the phone manufacturer exposes it.
- Explains the limits of per-client bandwidth control on standard Android.
- Requests no dangerous permissions, root, VPN, or bootloader unlock.

## Important limitation
A normal third-party Android app cannot universally enumerate hotspot clients, disconnect arbitrary clients, or enforce per-client Mbps limits. Those controls require manufacturer-specific privileged APIs/firmware support. This starter does not pretend to apply a speed cap when the OS does not expose one.

If the phone has no built-in per-device speed control, a compatible router with QoS/bandwidth limits is the no-root alternative. Clients must connect to that router's Wi-Fi for its per-device limits to apply; clients connected directly to the phone hotspot are not controlled by the router.
