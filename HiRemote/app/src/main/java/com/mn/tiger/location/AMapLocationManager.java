package com.mn.tiger.location;

import android.location.Location;
import android.os.Bundle;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.location.LocationProviderProxy;
import com.mn.tiger.app.TGApplication;
import com.mn.tiger.log.Logger;

/**
 * Created by Dalang on 2015/7/26.
 * 百度定位管理类
 */
public class AMapLocationManager implements ILocationManager
{
    private static final Logger LOG = Logger.getLogger(AMapLocationManager.class);

    private LocationManagerProxy locationManagerProxy;

    private ILocationListener listener;

    private AMapLocationListener locationListener = new AMapLocationListener()
    {
        @Override
        public void onLocationChanged(final AMapLocation aMapLocation)
        {
            LOG.d("[Method:onLocationChanged] Provider == " + aMapLocation.getProvider());

            if(aMapLocation.getProvider() == LocationManagerProxy.GPS_PROVIDER)
            {
                //使用高德地址解析功能在进行解析地址
                AMapGeoCoding.geoCoding(aMapLocation.getLatitude(), aMapLocation.getLongitude(), new AMapGeoCoding.GeoCodeListener()
                {
                    @Override
                    public void onGeoCodingSuccess(GeoCodeResult result)
                    {
                        LOG.d("[Method:onGeoCodingSuccess]");
                        if(null != listener)
                        {
                            TGLocation tgLocation = TGLocation.initWith(aMapLocation);
                            tgLocation.setTime(System.currentTimeMillis());
                            if(result.getResults().size() > 0)
                            {
                                AddressResult addressResult = result.getResults().get(0);
                                //TODO 分析数据
                                tgLocation.setCountry(addressResult.getFormatted_address());
                            }
                            listener.onReceiveLocation(tgLocation);
                        }
                    }

                    @Override
                    public void onGeoCodingError(int code, String message)
                    {
                        LOG.e("[Method:onGeoCodingError] " + message);
                    }
                });
            }
            else
            {
                listener.onReceiveLocation(TGLocation.initWith(aMapLocation));
            }
        }

        @Override
        public void onLocationChanged(Location location)
        {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
        }

        @Override
        public void onProviderEnabled(String provider)
        {
        }

        @Override
        public void onProviderDisabled(String provider)
        {
        }
    };

    /**
     * 初始化
     */
    AMapLocationManager()
    {
        locationManagerProxy = locationManagerProxy.getInstance(TGApplication.getInstance());
    }

    /**
     * 请求定位
     */
    @Override
    public void requestLocationUpdates()
    {
        locationManagerProxy.requestLocationData(LocationProviderProxy.AMapNetwork, -1 , 50 , locationListener);
        LOG.d("[Method:requestLocationUpdates]");
    }

    @Override
    public void setLocationListener(ILocationListener listener)
    {
        this.listener = listener;
    }

    public boolean isLocationInChina(TGLocation location)
    {
        return true;
    }
}