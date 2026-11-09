package com.glassui.lock

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class LockService : Service() {
    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            c.startActivity(Intent(c, LockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("lock", "Glass Lock", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "lock")
            .setSmallIcon(android.R.drawable.ic_lock_lock).setContentTitle("Glass Lock active").build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
        registerReceiver(rx, IntentFilter(Intent.ACTION_SCREEN_ON))
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onDestroy() { unregisterReceiver(rx); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}
