package com.mn.tiger.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;

import com.czt.mp3recorder.MP3Recorder;
import com.mn.tiger.app.TGApplication;
import com.mn.tiger.bluetooth.TGBluetoothManager;
import com.mn.tiger.log.Logger;
import com.mn.tiger.utility.FileUtils;

import java.io.IOException;

/**
 * Created by peng on 15/8/2.
 */
public class TGRecorder
{
    private static final Logger LOG = Logger.getLogger(TGRecorder.class);

    private volatile static TGRecorder instance;

    private static Handler timeHandler;

    private AudioManager audioManager;

    private MP3Recorder mp3Recorder;

    private String currentRecordFilePath;

    private OnRecordListener onRecordListener;

    private int recordDuration;

    private static final int DURATION_INTERVAL = 200;

    public static TGRecorder getInstance()
    {
        if(null == instance)
        {
            synchronized (TGRecorder.class)
            {
                if(null == instance)
                {
                    instance = new TGRecorder();
                }
            }
        }
        return instance;
    }

    private TGRecorder()
    {
        audioManager = (AudioManager)TGApplication.getInstance().getSystemService(Context.AUDIO_SERVICE);
        timeHandler = new Handler();
    }

    public void start(String outputFilePath)
    {
        this.start(outputFilePath, null);
    }

    public void start(String outputFilePath, OnRecordListener listener)
    {
        LOG.d("[Method:start] filePath ==  " + outputFilePath);

        this.currentRecordFilePath = outputFilePath;
        this.onRecordListener = listener;

        mp3Recorder = new MP3Recorder(FileUtils.createFile(outputFilePath));
        if(TGBluetoothManager.isBlueToothHeadsetConnected())
        {
            startRecordUseByToothHeadSet();
        }
        else
        {
            executeRecord();
        }
    }

    private void startRecordUseByToothHeadSet()
    {
        LOG.e("[Method:startRecordUseByToothHeadSet]");
        if(!audioManager.isBluetoothScoAvailableOffCall())
        {
            LOG.e("[Method:startRecordUseByToothHeadSet] not support bluetooth record");
            return;
        }

        if(audioManager.isBluetoothScoOn())
        {
            executeRecord();
        }
        else
        {
            TGApplication.getInstance().registerReceiver(new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                    {
                        if(state == AudioManager.SCO_AUDIO_STATE_CONNECTED)
                        {
                            audioManager.setBluetoothScoOn(true);
                            executeRecord();
                            TGApplication.getInstance().unregisterReceiver(this);
                        }
                    }
                }
            }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));

            audioManager.startBluetoothSco();
        }
    }

    private void executeRecord()
    {
        LOG.e("[Method:executeRecord]");
        try
        {
            mp3Recorder.start();
            if(null != onRecordListener)
            {
                onRecordListener.onRecordStart(currentRecordFilePath);
            }
            recordDuration = 0;
            timeHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    recordDuration += DURATION_INTERVAL;
                    if (null != onRecordListener)
                    {
                        onRecordListener.onRecording(currentRecordFilePath, recordDuration);
                    }

                    if(isRecording())
                    {
                        timeHandler.postDelayed(this, DURATION_INTERVAL);
                    }
                }
            }, DURATION_INTERVAL);
        }
        catch (IOException e)
        {
            LOG.e(e);
        }
    }

    public void stop()
    {
        LOG.d("[Method:stop]");
        if(null != mp3Recorder)
        {
            mp3Recorder.stop();
            if(TGBluetoothManager.isBlueToothHeadsetConnected())
            {
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
            }

            if(null != onRecordListener)
            {
                onRecordListener.onRecordStop(currentRecordFilePath);
            }
        }
    }

    public boolean isRecording()
    {
        if(null != mp3Recorder)
        {
            return mp3Recorder.isRecording();
        }
        return false;
    }


    public static interface OnRecordListener
    {
        void onRecordStart(String outputFilePath);

        void onRecording(String outputFilePath, int duration);

        void onRecordStop(String outputFilePath);
    }
}
