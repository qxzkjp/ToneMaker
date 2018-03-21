package com.example.deus.tonemaker;

import java.util.Arrays;

/**
 * Created by Deus on 05/02/2018.
 */

public class ToneDispatcher {
    private int freq;
    private int batchesPerSecond;
    private int sampleRate;
    private byte samples[];
    private byte initialBatch[];
    private int batch;
    private int samplesPerBatch;
    private static int bytesPerSample=2;

    private void setup(int fIn, int srIn, int bpsIn){
        freq=fIn;
        sampleRate=srIn;
        batchesPerSecond=bpsIn;
        batch=0;
        samplesPerBatch=sampleRate/batchesPerSecond;
        samples=new byte[sampleRate*bytesPerSample];
        initialBatch=new byte[samplesPerBatch*bytesPerSample*2];
        setFreq(fIn);
    }

    public ToneDispatcher(int fIn, int srIn, int bpsIn){
        setup(fIn,srIn,bpsIn);
    }

    public ToneDispatcher(){
        setup(440,44000,100);
    }

    public void setFreq(int fIn){
        freq=fIn;
        for(int i=0;i<sampleRate;++i){
            double tmp=Math.sin(fIn * 2 * Math.PI * i / sampleRate);
            if(i<sampleRate/2){
                tmp = tmp * (i*2)/sampleRate;
            }else{
                tmp = tmp * 2 * (sampleRate-i)/sampleRate;
            }
            // scale to maximum amplitude
            final short val = (short) ((tmp * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            samples[i*2] = (byte) (val & 0x00ff);
            samples[i*2+1] = (byte) ((val & 0xff00) >>> 8);
            if(i<samplesPerBatch*2){
                //ramp up volume for initial batch
                short nVal = (short) (val * i / (samplesPerBatch*2));
                initialBatch[i*2] = (byte) (nVal & 0x00ff);
                initialBatch[i*2+1] = (byte) ((nVal & 0xff00) >>> 8);
            }
        }
    }

    public byte[] getNextBatch(){
        byte ret[]= Arrays.copyOfRange(
                samples,
                batch*samplesPerBatch*bytesPerSample,
                (batch+1)*samplesPerBatch*bytesPerSample
        );
        batch=(batch+1)%batchesPerSecond;
        return ret;
    }

    //initial batch is a double batch for reasons
    public byte[] getInitialBatch(){
        batch=2;
        byte ret[]=initialBatch.clone();
        return ret;
    }

    public byte[] getWholeSecond(){
        return samples.clone();
    }
}
