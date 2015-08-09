package com.mn.tiger.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

import com.mn.tiger.app.TGApplication;

/**
 * Created by Dalang on 2015/7/26.
 */
public class GoogleLocationManager implements ILocationManager
{
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /**
     * 上一次定位的地址
     */
    private Location lastLocation;

    /**
     * 系统的位置管理器
     */
    private LocationManager locationManager;

    private ILocationListener listener;

    public GoogleLocationManager()
    {
        locationManager = (LocationManager) TGApplication.getInstance().getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * 请求地址更新
     */
    @Override
    public void requestLocationUpdates()
    {
        requestGPSLocationUpdates();
        requestNetworkLocationUpdates();
    }

    @Override
    public void setLocationListener(ILocationListener listener)
    {
        this.listener = listener;
    }

    /**
     * 请求GPS定位更新
     */
    private void requestGPSLocationUpdates()
    {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2 * 1000, 20, gpsLocationListener);
    }

    /**
     * 请求网络定位地址更新
     */
    private void requestNetworkLocationUpdates()
    {
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2 * 1000, 20, networkLocationListener);
    }

    /**
     * 删除网络定位监听器
     */
    private void removeNetworkLocationUpdates()
    {
        locationManager.removeUpdates(networkLocationListener);
    }

    /**
     * gps定位监听对象
     */
    private LocationListener gpsLocationListener = new LocationListener()
    {
        /**
         * 是否已删除网络定位监听器的控制参数
         */
        private boolean isRemoveNetworkListener = false;

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            //若GPS定位不可用，则启动网络定位
            if (LocationProvider.OUT_OF_SERVICE == status)
            {
                requestNetworkLocationUpdates();
                isRemoveNetworkListener = false;
            }
        }

        @Override
        public void onProviderEnabled(String provider)
        {
        }

        @Override
        public void onProviderDisabled(String provider)
        {
        }

        @Override
        public void onLocationChanged(Location location)
        {
            //判断当前地址和上一次定位的结果哪个更加精确，若当前定位的地址更加精确，通知更新地址
            if (isBetterLocation(location, lastLocation))
            {
                updateLocation(location);
            }

            //删除网络定位监听接口
            if (location != null && !isRemoveNetworkListener)
            {
                removeNetworkLocationUpdates();
                isRemoveNetworkListener = true;
            }
        }
    };

    /**
     * 网络定位监听类
     */
    private LocationListener networkLocationListener = new LocationListener()
    {
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

        @Override
        public void onLocationChanged(Location location)
        {
            //判断当前地址和上一次定位的结果哪个更加精确，若当前定位的地址更加精确，通知更新地址
            if (isBetterLocation(location, lastLocation))
            {
                updateLocation(location);
            }
        }
    };

    /**
     * 更新地址
     * @param location
     */
    private void updateLocation(final Location location)
    {
        this.lastLocation = location;
        GoogleGeoCoding.geoCoding(location.getLatitude(), location.getLongitude(), new GoogleGeoCoding.GeoCodeListener()
        {
            @Override
            public void onGeoCodingSuccess(GeoCodeResult result)
            {
                //发通知界面处理
                if (null != listener)
                {
                    TGLocation tgLocation = TGLocation.initWith(location);
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

            }
        });

    }

    /**
     * Determines whether one Location reading is better than the current
     * Location fix
     *(该方法由Google提供)
     * @param location
     *            The new Location that you want to evaluate
     * @param currentBestLocation
     *            The current Location fix, to which you want to compare the new
     *            one
     */
    private boolean isBetterLocation(Location location, Location currentBestLocation)
    {
        if (currentBestLocation == null)
        {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use
        // the new location
        // because the user has likely moved
        if (isSignificantlyNewer)
        {
            return true;
            // If the new location is more than two minutes older, it must be
            // worse
        }
        else if (isSignificantlyOlder)
        {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and
        // accuracy
        if (isMoreAccurate)
        {
            return true;
        }
        else if (isNewer && !isLessAccurate)
        {
            return true;
        }
        else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
        {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2)
    {
        if (provider1 == null)
        {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


}