package com.example.deus.tonemaker;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.PowerManager;

import static android.content.Context.POWER_SERVICE;

/**
 * Created by Deus on 05/02/2018.
 */

public class ToneSounder {
    private AudioTrack stream;
    private boolean isPlaying;
    private int samplesPerBatch;
    private int sampleRate;
    private int frequency;
    private int bps;
    private float volume;
    private final int bytesPerSample=2;
    public ToneSounder(int freq, int sampleRateIn, int batchesPerSecond) throws Exception{
        bps=batchesPerSecond;
        frequency=freq;
        sampleRate=sampleRateIn;
        samplesPerBatch = sampleRate / batchesPerSecond;
        if (sampleRate % batchesPerSecond != 0){
            throw new Exception("bad samples!");
        }
        int bufferSize = Math.max(
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT),
                (int)sampleRate*bytesPerSample/10);
        stream = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                AudioTrack.MODE_STREAM);
    }
    public void play(final Context ctx){
        isPlaying=true;
        Thread playerWorker = new Thread(new Runnable() {
            public void run() {
                while (isPlaying) {
                    syncPlay(ctx);
                }
            }
        });

        playerWorker.start();
    }

    public void syncPlay(Context ctx){
        //stream.getBufferSizeInFrames()
        PowerManager powerManager = (PowerManager) ctx.getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "playbackLock");
        wakeLock.acquire();
        isPlaying=true;
        stream.stop();
        stream.flush();
        stream.play();
        ToneDispatcher innerDispatcher = new ToneDispatcher(frequency, sampleRate, bps);
        while (isPlaying) {
            byte tmp[]=innerDispatcher.getNextBatch();
            stream.write(tmp,
                    0,tmp.length);
        }
        wakeLock.release();
    }

    public void setFrequency(int fIn){
        if(!isPlaying){
            frequency=fIn;
        }
    }

    public void setVolume(float newVolume){
        volume=newVolume;
        stream.setStereoVolume(newVolume, newVolume);
    }

    public boolean isPlaying(){
        return isPlaying;
    }
    public void stop(){
        stream.stop();
        isPlaying=false;
    }
};
