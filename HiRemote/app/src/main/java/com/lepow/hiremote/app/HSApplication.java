package com.lepow.hiremote.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;

import com.lepow.hiremote.bluetooth.HSBLEPeripheralManager;
import com.lepow.hiremote.bluetooth.data.PeripheralDataManager;
import com.lepow.hiremote.bluetooth.data.PeripheralInfo;
import com.lepow.hiremote.home.AlarmAlertActivity;
import com.lepow.hiremote.lbs.data.LocationDataManager;
import com.lepow.hiremote.lbs.data.LocationInfo;
import com.lepow.hiremote.misc.IntentAction;
import com.lepow.hiremote.request.HttpLoader;
import com.lepow.hiremote.upgrade.HSUpgradeDataParser;
import com.mn.tiger.app.TGApplicationProxy;
import com.mn.tiger.app.TGMultiDexApplication;
import com.mn.tiger.bluetooth.TGBLEManager;
import com.mn.tiger.bluetooth.TGBluetoothManager;
import com.mn.tiger.location.ILocationManager;
import com.mn.tiger.location.TGLocation;
import com.mn.tiger.location.TGLocationManager;
import com.mn.tiger.log.LogLevel;
import com.mn.tiger.log.Logger;
import com.mn.tiger.media.TGAudioPlayer;
import com.mn.tiger.request.HttpType;
import com.mn.tiger.upgrade.TGUpgradeManager;

import java.io.IOException;

public class HSApplication extends TGMultiDexApplication implements TGAudioPlayer.OnPlayListener
{
    private static final Logger LOG = Logger.getLogger(HSApplication.class);

    @Override
    public void onCreate()
    {
        super.onCreate();

        Logger.setGlobalLogLevel(LogLevel.LOG_LEVEL_DEBUG);

        HttpLoader<Void> httpLoader = new HttpLoader<Void>();
        httpLoader.addRequestParam("appId", getPackageName());
        httpLoader.setHttpType(HttpType.REQUEST_POST);

        TGUpgradeManager.setUpgradeDataParser(new HSUpgradeDataParser());
        TGUpgradeManager.setCheckUpgradeHttpLoader(httpLoader);

        //初始化合适的位置管理器
        TGLocationManager.init(ILocationManager.Provider.AMap);
        TGLocationManager.getInstance().initAppropriateLocationManager();

        //注册广播更新设备的电量
        this.registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                PeripheralDataManager.savePeripheral(HSApplication.this, PeripheralInfo.fromBLEPeripheralInfo(
                        HSBLEPeripheralManager.getInstance().getCurrentPeripheral()));
            }
        }, new IntentFilter(IntentAction.ACTION_READ_PERIPHERAL_POWER));

        //注册广播处理蓝牙设备断开时保存地理位置
        this.registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                int bleState = TGBLEManager.getBLEState(intent);
                if(bleState == TGBLEManager.BLE_STATE_DISCONNECTED)
                {
                    //请求设备断开地址
                    requestDisconnectedLocation();
                }
            }
        },new IntentFilter(TGBLEManager.ACTION_BLE_STATE_CHANGE));

        //注册广播播放蓝牙设备长按时的警报
        this.registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                AssetManager assetManager = getAssets();
                try
                {
                    AssetFileDescriptor dataSource = assetManager.openFd("alarm.mp3");
                    //强制声音从手机扬声器播放
                    TGAudioPlayer.getInstance().start(dataSource.getFileDescriptor(), HSApplication.this, true);
                    Intent startIntent = new Intent(HSApplication.this, AlarmAlertActivity.class);
                    startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startIntent);
                }
                catch (IOException e)
                {

                }
            }
        }, new IntentFilter(IntentAction.ACTION_ALARM));

    }

    /**
     * 请求设备失去连接时的地理位置
     */
    private void requestDisconnectedLocation()
    {
        LOG.d("[Method:requestDisconnectedLocation]");
        TGLocationManager locationManager = TGLocationManager.getInstance();
        locationManager.setLocationListener(new ILocationManager.ILocationListener()
        {
            @Override
            public void onReceiveLocation(TGLocation location)
            {
                LocationInfo locationInfo = LocationInfo.fromLocation(location);
                LocationDataManager.getInstance().saveDisconnectedLocation(HSApplication.this, locationInfo);
                LOG.d("[Method:onReceiveLocation] " + locationInfo.getAddress());

                Intent intent = new Intent(IntentAction.ACTION_DISCONNECTED_LOCATION);
                sendBroadcast(intent);
            }
        });

        locationManager.requestLocationUpdates();
    }

    @Override
    public void onPlayStart(String dataSource)
    {
    }

    @Override
    public void onPlaying(String dataSource, int playDuration, int audioDuration)
    {
    }

    @Override
    public void onPlayPause(String dataSource)
    {
    }

    @Override
    public void onPlayStop(String dataSource)
    {
        changeAudioOutput();
    }

    @Override
    public void onPlayComplete(String dataSource)
    {
        changeAudioOutput();
    }

    private void changeAudioOutput()
    {
        AudioManager audioManager = (AudioManager) TGApplicationProxy.getInstance().getApplication().getSystemService(Context.AUDIO_SERVICE);
        //设置声音强制从扬声器输出
        if(audioManager.isWiredHeadsetOn() || TGBluetoothManager.isBlueToothHeadsetConnected())
        {
            audioManager.setBluetoothScoOn(true);
            audioManager.setMicrophoneMute(true);
            audioManager.setSpeakerphoneOn(false);
        }

        audioManager.setMode(AudioManager.STREAM_MUSIC);
    }
}
