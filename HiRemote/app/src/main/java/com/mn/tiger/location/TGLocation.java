package com.mn.tiger.location;

import android.location.Location;

import com.amap.api.location.AMapLocation;

import java.io.Serializable;

/**
 * Created by Dalang on 2015/7/30.
 */
public class TGLocation implements Serializable
{
    private double latitude;

    private double longitude;

    private String city = "";

    private String province = "";

    private String country = "";

    private String street = "";

    private String address = "";

    private long time;

    private Location location;

    private TGLocation()
    {
    }

    static TGLocation initWith(Location location)
    {
        if(null != location)
        {
            TGLocation tgLocation = new TGLocation();
            tgLocation.latitude = location.getLatitude();
            tgLocation.longitude = location.getLongitude();
            tgLocation.location = location;
            return tgLocation;
        }
        return null;
    }

    static TGLocation initWith(AMapLocation location)
    {
        if(null != location)
        {
            TGLocation tgLocation = new TGLocation();
            tgLocation.latitude = location.getLatitude();
            tgLocation.longitude = location.getLongitude();
            tgLocation.city = location.getCity();
            tgLocation.country = location.getCountry();
            tgLocation.province = location.getProvince();
            tgLocation.address = location.getAddress();
            tgLocation.street = location.getStreet();
            return tgLocation;
        }
        return null;
    };

    public double getLatitude()
    {
        return latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public String getCity()
    {
        return city;
    }

    void setCity(String city)
    {
        this.city = city;
    }

    public String getProvince()
    {
        return province;
    }

    void setProvince(String province)
    {
        this.province = province;
    }

    public String getCountry()
    {
        return country;
    }

    void setCountry(String country)
    {
        this.country = country;
    }

    public String getStreet()
    {
        return street;
    }

    void setStreet(String street)
    {
        this.street = street;
    }

    public String getAddress()
    {
        return address;
    }

    void setAddress(String address)
    {
        this.address = address;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public long getTime()
    {
        return time;
    }

    public Location getLocation()
    {
        return location;
    }
}
