package com.opencode.ramguard;
import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class GuardService extends Service {
    public static final String CHANNEL = "ramguard_channel";
    @Override public void onCreate() { super.onCreate(); createChannel(); startForeground(1, build()); }
    @Override public int onStartCommand(Intent i, int f, int s) { return START_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "RamGuard", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Tap to open RamGuard settings");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification build() {
        Intent it = new Intent(this, ConfigActivity.class);
        int fl = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) fl |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 0, it, fl);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Notification n = b.setContentTitle("RamGuard active")
                .setContentText("Tap to configure game memory saver")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentIntent(pi).setOngoing(true).setAutoCancel(false).build();
        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT
                | Notification.FLAG_FOREGROUND_SERVICE;
        return n;
    }
}
