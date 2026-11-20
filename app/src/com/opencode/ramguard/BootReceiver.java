package com.opencode.ramguard;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) {
            try { c.startForegroundService(new Intent(c, GuardService.class)); } catch (Exception ignore) {}
        }
    }
}
