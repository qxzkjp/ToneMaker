package com.example.deus.tonemaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.POWER_SERVICE;
import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;

public class StartupReceiver extends BroadcastReceiver {
    static private PowerManager.WakeLock wakeLock=null;
    static private int nextSerial=0;
    static private Set pendingIntents=new HashSet();
    static private ReentrantLock intentLock = new ReentrantLock();
    @Override
    public void onReceive(Context context, Intent intent) {
        if(wakeLock==null){
            PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "startupLock");
        }
        wakeLock.acquire();
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())){
            SharedPreferences prefs =
                    context.getSharedPreferences("default",MODE_PRIVATE);
            if(prefs.getBoolean("onStartup", false)){
                Intent service = new Intent(context, ToneService.class);
                service.setAction(ToneService.ACTION_PLAY);
                service.putExtra("onBoot",true);
                service.putExtra("frequency",
                        prefs.getInt("frequency", 440));
                service.putExtra("volume",
                        (float)(prefs.getInt("volume", 50)/100.0));
                service.putExtra("sentBy",
                        "com.example.deus.tonemaker.StartupReceiver.onReceive");
                intentLock.lock();
                service.putExtra("serialNumber", nextSerial);
                pendingIntents.add(nextSerial);
                ++nextSerial;
                intentLock.unlock();
                if(android.os.Build.VERSION.SDK_INT >= 26){
                    context.startForegroundService(service);
                }else{
                    context.startService(service);
                }
            }
        }
    }
    static public void releaseLock(Intent intent)
    {
        String id=intent.getStringExtra("sentBy");
        if(id==null || id.equals("com.example.deus.tonemaker.StartupReceiver.onReceive")){
            intentLock.lock();
            int serial=intent.getIntExtra("serialNumber",-1);
            if(pendingIntents.contains(serial)){
                wakeLock.release();
                pendingIntents.remove(serial);
            }
            intentLock.unlock();
        }
    }
}
