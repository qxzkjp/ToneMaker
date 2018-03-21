package com.example.deus.tonemaker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //private ContinuousBuzzer tonePlayer = new ContinuousBuzzer();
    private ToneSounder tonePlayer;
    private int defaultSampleRate=AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
    private EditText freqBox;
    private SeekBar seekBar;
    private SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "CreationLock");
        wakeLock.acquire();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button goButton = (Button)findViewById(R.id.goButton);
        goButton.setOnClickListener(this);
        CheckBox startupCheckBox =
                (CheckBox)findViewById(R.id.startupCheckBox);
        startupCheckBox.setOnClickListener(this);
        freqBox = (EditText)findViewById(R.id.freqBox);
        try{
            tonePlayer = new ToneSounder(440,
                    defaultSampleRate,
                    20);
        }catch(Exception e){
            Toast.makeText(getApplicationContext(), "Exception!",
                    Toast.LENGTH_SHORT).show();
        }
        // set a change listener on the SeekBar
        seekBar = findViewById(R.id.volumeSlider);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        ToneService.setVolume((float)(seekBar.getProgress()/100.0));
        prefs = getSharedPreferences("default",MODE_PRIVATE);
        int volume = prefs.getInt("volume",50);
        int freq = prefs.getInt("frequency",440);
        boolean onStartup = prefs.getBoolean("onStartup",false);
        seekBar.setProgress(volume);
        freqBox.setText(Integer.toString(freq));
        startupCheckBox.setChecked(onStartup);
    }

    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // updated continuously as the user slides the thumb
            ToneService.setVolume((float)(progress/100.0));
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("volume",progress);
            editor.commit();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // called when the user first touches the SeekBar
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // called after the user finishes moving the SeekBar
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case  R.id.goButton: {
                SharedPreferences.Editor editor = prefs.edit();
                String freqstr=freqBox.getText().toString();
                int freq;
                if(freqstr.equals(""))
                {
                    freq = 440;
                    freqBox.setText("440");
                }else{
                    freq = Integer.parseInt(freqstr);
                }
                editor.putInt("frequency",freq);
                float vol = (float)(seekBar.getProgress()/100.0);
                if(ToneService.isPlaying()){
                    Intent intent=new Intent(this, ToneService.class);
                    intent.setAction(ToneService.ACTION_STOP);
                    startService(intent);
                    Toast.makeText(getApplicationContext(), "Tone stopped.",
                            Toast.LENGTH_SHORT).show();
                    freqBox.setFocusableInTouchMode(true);
                }else {
                    Intent intent=new Intent(this, ToneService.class);
                    intent.setAction(ToneService.ACTION_PLAY);
                    intent.putExtra("frequency",freq);
                    intent.putExtra("batchesPerSecond",20);
                    intent.putExtra("sampleRate",defaultSampleRate);
                    intent.putExtra("volume",vol);
                    startService(intent);
                    Toast.makeText(getApplicationContext(), "Tone playing.",
                            Toast.LENGTH_SHORT).show();
                    freqBox.setFocusable(false);
                }
                editor.commit();
                break;
            }
            case R.id.startupCheckBox: {
                SharedPreferences.Editor editor = prefs.edit();
                if(prefs.getBoolean("onStartup",false)){
                    editor.putBoolean("onStartup",false);
                }else{
                    editor.putBoolean("onStartup",true);
                }
                editor.commit();
                break;
            }
        }
    }
}
