package com.example.deus.tonemaker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

public class ToneService extends Service {
    private static ToneSounder buzzer;
    private static float volume=1.0f;
    public final static String ACTION_PLAY = "com.example.deus.tonemaker.intent.start";
    public final static String ACTION_STOP = "com.example.deus.tonemaker.intent.stop";
    public ToneService() {
    }
    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        //HandlerThread thread = new HandlerThread("ServiceStartArguments",
        //        Process.THREAD_PRIORITY_BACKGROUND);
        //thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        //mServiceLooper = thread.getLooper();
        //mServiceHandler = new ServiceHandler(mServiceLooper);
        if( android.os.Build.VERSION.SDK_INT >= 26){
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
            String id = "service_channel";
            CharSequence name = "Tonemaker";
            String description = "Service notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mNotificationManager.createNotificationChannel(mChannel);
            new NotificationCompat.Builder(this, "service_channel");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "serviceLock");
        wakeLock.acquire();
        StartupReceiver.releaseLock(intent);
        if(intent.getAction()==ACTION_PLAY && !isPlaying()){
            NotificationCompat.Builder builder;
            if( android.os.Build.VERSION.SDK_INT >= 26) {
                builder=new NotificationCompat.Builder(this,
                        "service_channel");
            }else{
                builder=new NotificationCompat.Builder(this, "low");
            }
            Notification notification =
                    builder
                            .setSmallIcon(R.drawable.ic_tuning_fork)
                            .setContentTitle("Tonemaker")
                            .setContentText("Playing tone...").build();
            startForeground(1, notification);
            int defaultSampleRate =
                    AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
            int sr = intent.getIntExtra("sampleRate", defaultSampleRate);
            int bps = intent.getIntExtra("batchesPerSecond", 20);
            int freq = intent.getIntExtra("frequency", 440);
            volume = intent.getFloatExtra("volume", 0.5f);
            try{
                buzzer=new ToneSounder(freq,sr,bps);
                buzzer.setVolume(volume);
                buzzer.play(this);
            }catch(Exception e){

            }
        }else if(intent.getAction()==ACTION_STOP && isPlaying()){
            buzzer.stop();
            stopSelf();
        }
        wakeLock.release();
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    public static boolean isPlaying(){
        if(buzzer!=null) {
            return buzzer.isPlaying();
        }else{
            return false;
        }
    }

    public static void setVolume(float newVolume){
        volume=newVolume;
        if(isPlaying()){
            buzzer.setVolume(newVolume);
        }
    }

    @Override
    public void onDestroy(){
        if(android.os.Build.VERSION.SDK_INT >= 26){
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
            String id = "service_channel";
            mNotificationManager.deleteNotificationChannel(id);
        }
    }

}
