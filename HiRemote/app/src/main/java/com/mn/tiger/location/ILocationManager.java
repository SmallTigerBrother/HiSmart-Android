package com.mn.tiger.location;

/**
 * Created by Dalang on 2015/7/30.
 */
public interface ILocationManager
{
    void requestLocationUpdates();

    void removeLocationUpdates();

    void destroy();

    void setLocationListener(ILocationListener listener);

    TGLocation getLastLocation();

    boolean isLocationInChina(TGLocation location);

    interface ILocationListener
    {
        void onReceiveLocation(TGLocation location);
    }

    enum Provider
    {
        BaiDu,
        Google,
        AMap
    }
}
