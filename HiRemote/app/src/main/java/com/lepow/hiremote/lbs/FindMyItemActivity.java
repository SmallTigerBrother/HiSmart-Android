package com.lepow.hiremote.lbs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.lepow.hiremote.R;
import com.lepow.hiremote.app.BaseActivity;
import com.lepow.hiremote.bluetooth.HSBLEPeripheralManager;
import com.lepow.hiremote.bluetooth.data.PeripheralInfo;
import com.lepow.hiremote.lbs.data.LocationDataManager;
import com.lepow.hiremote.lbs.data.LocationInfo;
import com.lepow.hiremote.misc.ActivityResultCode;
import com.lepow.hiremote.misc.IntentKeys;
import com.mn.tiger.bluetooth.TGBLEManager;
import com.mn.tiger.location.TGLocationManager;
import com.mn.tiger.log.Logger;
import com.mn.tiger.map.AMapManager;
import com.mn.tiger.map.GoogleMapManager;
import com.mn.tiger.map.IMapManager;
import com.mn.tiger.widget.TGNavigationBar;
import com.mn.tiger.widget.imageview.CircleImageView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by peng on 15/7/21.
 */
public class FindMyItemActivity extends BaseActivity implements View.OnClickListener
{
    private static final Logger LOG = Logger.getLogger(FindMyItemActivity.class);

    @Bind(R.id.device_avatar)
    CircleImageView deviceAvatarView;

    @Bind(R.id.peripheral_name)
    TextView deviceName;

    @Bind(R.id.device_connect_status)
    ImageView connectStatusView;

    @Bind(R.id.device_location)
    TextView deviceLocationView;

    @Bind(R.id.mapview_container)
    FrameLayout mapContainer;

    PeripheralInfo peripheralInfo;

    private LocationInfo locationInfo;

    private IMapManager mapManager;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            LOG.i("[Method:broadcastReceiver:onReceive] ");
            int bleState = TGBLEManager.getBLEState(intent);
            switch (bleState)
            {
                case TGBLEManager.BLE_STATE_CONNECTED:
                case TGBLEManager.BLE_STATE_DISCONNECTED:
                    initPeripheralStatusView();
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.find_my_item_layout);
        ButterKnife.bind(this);
        setBarTitleText(getString(R.string.find_my_item_title));
        showRightBarButton(true);
        getRightBarButton().setText(getString(R.string.history));
        getRightBarButton().setOnClickListener(this);

        initPeripheralStatusView();

        registerReceiver(broadcastReceiver, new IntentFilter(TGBLEManager.ACTION_BLE_STATE_CHANGE));

        locationInfo = LocationDataManager.getInstance().findLatestDisconnectedLocation(this);
        initMapView(savedInstanceState, locationInfo);
    }

    private void initPeripheralStatusView()
    {
        peripheralInfo = PeripheralInfo.fromBLEPeripheralInfo(HSBLEPeripheralManager.getInstance().getCurrentPeripheral());

        deviceName.setText(peripheralInfo.getPeripheralName());
        deviceLocationView.setText("");

        if(peripheralInfo.isConnected())
        {
            deviceAvatarView.setImageResource(R.drawable.icon_device);
            connectStatusView.setImageResource(R.drawable.connect_rssi_yes);
        }
        else
        {
            deviceAvatarView.setImageResource(R.drawable.icon_device_disconnected);
            connectStatusView.setImageResource(R.drawable.connect_rssi_no);
        }
    }

    private void initMapView(Bundle savedInstanceState, LocationInfo locationInfo)
    {
        if(TGLocationManager.getInstance().isCurrentLocationInChina())
        {
            LOG.i("[Method:initMapView] use AMapManager");
            mapManager = new AMapManager(this);
        }
        else
        {
            LOG.i("[Method:initMapView] use GoogleMapManager");
            mapManager = new GoogleMapManager(this);
        }

        mapManager.init(mapContainer, savedInstanceState);

        if(null == locationInfo)
        {
            locationInfo = LocationInfo.fromLocation(TGLocationManager.getInstance().getLastLocation());
        }

        if(null != locationInfo)
        {
            mapManager.addMarker(Double.valueOf(locationInfo.getLatitude()), Double.valueOf(locationInfo.getLongitude()),
                    locationInfo.getAddress());
            mapManager.centerTo(Double.valueOf(locationInfo.getLatitude()), Double.valueOf(locationInfo.getLongitude()));
        }

        mapManager.showMyLocation();
    }

    @Override
    protected void initNavigationResource(TGNavigationBar navigationBar)
    {
        super.initNavigationResource(navigationBar);
        navigationBar.setBackgroundResource(R.drawable.navi_bar_bg);
    }

    @OnClick({R.id.buzz_my_item, R.id.stop_buzz})
    public void onClick(View view)
    {
        if(view == getRightBarButton())
        {
            Intent intent = new Intent(this, DisconnectLocationHistory.class);
            startActivityForResult(intent, 0);
            return;
        }

        switch (view.getId())
        {
            case R.id.buzz_my_item:
                HSBLEPeripheralManager.getInstance().turnOnAlarmImmediately();
                break;

            case R.id.stop_buzz:
                HSBLEPeripheralManager.getInstance().turnOffAlarmImmediately();
                break;

            default:
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        mapManager.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mapManager.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mapManager.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mapManager.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        //TODO 刷新界面位置
        if(resultCode == ActivityResultCode.DISCONNECT_LOCATION_HISTORY)
        {
            locationInfo = (LocationInfo)data.getSerializableExtra(IntentKeys.LOCATION_INFO);
            if(null != locationInfo)
            {
                mapManager.clear();
                mapManager.addMarker(Double.valueOf(locationInfo.getLatitude()), Double.valueOf(locationInfo.getLongitude()),
                        locationInfo.getAddress());
                mapManager.centerTo(Double.valueOf(locationInfo.getLatitude()), Double.valueOf(locationInfo.getLongitude()));
            }
        }
    }
}
