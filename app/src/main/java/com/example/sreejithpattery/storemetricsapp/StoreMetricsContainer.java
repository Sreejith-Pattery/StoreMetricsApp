package com.example.sreejithpattery.storemetricsapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by 310722 on 4/26/2016.
 */
public class StoreMetricsContainer
{
    private static long avlMem;
    private static long totMem;
    private static long usedMem;

    private SensorManager sensorManager;
    private Sensor accelorometer;
    private LocationManager locationManager;


    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 1; //


    float[] mGravity;
    float mAccelLast = 0;
    float mAccelCurrent = 0;
    float mAccel = 0;
    private Handler handler;
    private Runnable r;
    private boolean shouldThreadRun = true;
    private ArrayAdapter<DeviceMetric> otherListAdapter;

    private Tracker mTracker;
    GoogleAnalytics analytics;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude;
    private double currentLongitude;



    public static DeviceMetric deviceStrgPerc;
    public static DeviceMetric  deviceStrgMB;
    public static DeviceMetric  frgrdAppID;
    public static DeviceMetric  frgrdAppName;
    public static DeviceMetric  wifiName;
    public static DeviceMetric  wifiStrngth;
    public static DeviceMetric  deviceIP;
    public static DeviceMetric  deviceGatewayMetric;
    public static DeviceMetric  deviceSerial;
    public static DeviceMetric  screenStateMetric;
    public static DeviceMetric  screenBrightness;
    public static DeviceMetric  foreGroundAppVersion;
    public static DeviceMetric  cpuInfo;
    public static DeviceMetric  ramInfo;
    public static DeviceMetric  ramPerc;
    public static DeviceMetric  isMoving;
    public static DeviceMetric  latitude;
    public static DeviceMetric  longitude;
    public static DeviceMetric  batteryHealth;
    public static DeviceMetric  batteryChargeLvl;
    public static DeviceMetric  batteryTmp;
    public static DeviceMetric  batteryChrgSts;
    public static DeviceMetric  batteryMfd;
    public static DeviceMetric  batterySerial;
    GetStoreMetrics getStoreMetrics;
    public static Context Currcontext;
    private List<DeviceMetric> otherMetrics = new ArrayList<>();
    public static boolean hasInitialized = false;

    public StoreMetricsContainer(Context context)
    {
        cpuInfo = new DeviceMetric("Current CPU Usage %","");
        ramInfo = new DeviceMetric("RAM Usage MB","");
        ramPerc = new DeviceMetric("RAM Usage %","");
        isMoving = new DeviceMetric("Device Movement","");
        latitude = new DeviceMetric("Device latitude","");
        longitude = new DeviceMetric("Device longitude","");
        batteryHealth = new DeviceMetric("Battery Health","");
        batteryChargeLvl = new DeviceMetric("Battery Charge Level","");
        batteryTmp = new DeviceMetric("Battery Temperature","");
        batteryChrgSts = new DeviceMetric("Device charging status","");
        batteryMfd = new DeviceMetric("Battery Manufacture Date","");
        batterySerial = new DeviceMetric("Battery Serial Number","");
        deviceStrgPerc = new DeviceMetric("Device Storage Remaining %","");
        deviceStrgMB = new DeviceMetric("Device Storage Remaining MB","");
        frgrdAppID = new DeviceMetric("Foreground Application Bundle ID","");
        frgrdAppName = new DeviceMetric("Foreground Application Name","");
        wifiName = new DeviceMetric("Wifi Network","");
        wifiStrngth = new DeviceMetric("Wifi Signal Strength","");
        deviceIP = new DeviceMetric("Device IP","");
        deviceGatewayMetric = new DeviceMetric("Device Gateway","");
        deviceSerial = new DeviceMetric("Device Serial Number","");
        screenStateMetric = new DeviceMetric("Screen State","");
        screenBrightness = new DeviceMetric("Screen Brightness","");
        foreGroundAppVersion = new DeviceMetric("Foreground Application version","");

        getStoreMetrics = new GetStoreMetrics(context);
        Currcontext = context;
        hasInitialized = false;
    }

    public static Context getContext()
    {
        return Currcontext;
    }

    public static DeviceMetric getCpuInfo() {
        return cpuInfo;
    }

    public void setCpuInfo(DeviceMetric cpuInfo) {
        this.cpuInfo = cpuInfo;
    }

    public static DeviceMetric getRamInfo() {
        return ramInfo;
    }

    public void setRamInfo(DeviceMetric ramInfo) {
        this.ramInfo = ramInfo;
    }

    public static DeviceMetric getRamPerc() {
        return ramPerc;
    }

    public void setRamPerc(DeviceMetric ramPerc) {
        this.ramPerc = ramPerc;
    }

    public static DeviceMetric getIsMoving() {
        return isMoving;
    }

    public void setIsMoving(DeviceMetric isMoving) {
        this.isMoving = isMoving;
    }

    public static DeviceMetric getLatitude() {
        return latitude;
    }

    public void setLatitude(DeviceMetric latitude) {
        this.latitude = latitude;
    }

    public static DeviceMetric getLongitude() {
        return longitude;
    }

    public void setLongitude(DeviceMetric longitude) {
        this.longitude = longitude;
    }

    public static DeviceMetric getBatteryHealth() {
        return batteryHealth;
    }

    public void setBatteryHealth(DeviceMetric batteryHealth) {
        this.batteryHealth = batteryHealth;
    }

    public static DeviceMetric getBatteryChargeLvl() {
        return batteryChargeLvl;
    }

    public void setBatteryChargeLvl(DeviceMetric batteryChargeLvl) {
        this.batteryChargeLvl = batteryChargeLvl;
    }

    public static DeviceMetric getBatteryTmp() {
        return batteryTmp;
    }

    public void setBatteryTmp(DeviceMetric batteryTmp) {
        this.batteryTmp = batteryTmp;
    }

    public static DeviceMetric getBatteryChrgSts() {
        return batteryChrgSts;
    }

    public void setBatteryChrgSts(DeviceMetric batteryChrgSts) {
        this.batteryChrgSts = batteryChrgSts;
    }

    public static DeviceMetric getBatteryMfd() {
        return batteryMfd;
    }

    public void setBatteryMfd(DeviceMetric batteryMfd) {
        this.batteryMfd = batteryMfd;
    }

    public static DeviceMetric getBatterySerial() {
        return batterySerial;
    }

    public void setBatterySerial(DeviceMetric batterySerial) {
        this.batterySerial = batterySerial;
    }

    public static DeviceMetric getDeviceStrgPerc() {
        return deviceStrgPerc;
    }

    public void setDeviceStrgPerc(DeviceMetric deviceStrgPerc) {
        this.deviceStrgPerc = deviceStrgPerc;
    }

    public static DeviceMetric getDeviceStrgMB() {
        return deviceStrgMB;
    }

    public void setDeviceStrgMB(DeviceMetric deviceStrgMB) {
        this.deviceStrgMB = deviceStrgMB;
    }

    public static DeviceMetric getFrgrdAppID() {
        return frgrdAppID;
    }

    public void setFrgrdAppID(DeviceMetric frgrdAppID) {
        this.frgrdAppID = frgrdAppID;
    }

    public static DeviceMetric getFrgrdAppName() {
        return frgrdAppName;
    }

    public void setFrgrdAppName(DeviceMetric frgrdAppName) {
        this.frgrdAppName = frgrdAppName;
    }

    public static DeviceMetric getWifiName() {
        return wifiName;
    }

    public void setWifiName(DeviceMetric wifiName) {
        this.wifiName = wifiName;
    }

    public static DeviceMetric getWifiStrngth() {
        return wifiStrngth;
    }

    public void setWifiStrngth(DeviceMetric wifiStrngth) {
        this.wifiStrngth = wifiStrngth;
    }

    public static DeviceMetric getDeviceIP() {
        return deviceIP;
    }

    public void setDeviceIP(DeviceMetric deviceIP) {
        this.deviceIP = deviceIP;
    }

    public void setDeviceGatewayMetric(DeviceMetric deviceGatewayMetric) {
        this.deviceGatewayMetric = deviceGatewayMetric;
    }


    public static DeviceMetric getDeviceSerial() {
        return deviceSerial;
    }

    public void setDeviceSerial(DeviceMetric deviceSerial) {
        this.deviceSerial = deviceSerial;
    }

    public void setScreenState(DeviceMetric screenState) {
        this.screenStateMetric = screenState;
    }

    public static DeviceMetric getScreenStateMetric() {
        return screenStateMetric;
    }

    public static DeviceMetric getScreenBrightness() {
        return screenBrightness;
    }

    public void setScreenBrightness(DeviceMetric screenBrightness) {
        this.screenBrightness = screenBrightness;
    }

    public static DeviceMetric getForeGroundAppVersion() {
        return foreGroundAppVersion;
    }

    public void setForeGroundAppVersion(DeviceMetric foreGroundAppVersion) {
        this.foreGroundAppVersion = foreGroundAppVersion;
    }


    public static DeviceMetric getDeviceGatewayMetric() {
        return deviceGatewayMetric;
    }


    public static void setDeviceMetrics()
    {
        GetStoreMetrics getStoreMetrics= new GetStoreMetrics(getContext());

        DeviceMetric deviceStrgPerc = getDeviceStrgPerc();
        deviceStrgPerc = GetStoreMetrics.getAvailableInternalMemorySizePerc(deviceStrgPerc);

        DeviceMetric wifiStrngth = getWifiStrngth();
        wifiStrngth = getStoreMetrics.getCurrentSignalStrength(wifiStrngth);

        DeviceMetric deviceStrgMB = getDeviceStrgMB();
        deviceStrgMB.setDeviceMetricValue(GetStoreMetrics.getAvailableInternalMemorySize() + " out of " + GetStoreMetrics.getTotalInternalMemorySize());
        deviceStrgMB.setIsAttentionRequired(0);

        String strForeGroundTask = null;
        DeviceMetric frgrdAppID = getFrgrdAppID();
        strForeGroundTask = getStoreMetrics.getForegroundTask();
        frgrdAppID.setDeviceMetricValue(strForeGroundTask);
        frgrdAppID.setIsAttentionRequired(0);

        DeviceMetric frgrdAppName = getFrgrdAppName();
        frgrdAppName.setDeviceMetricValue(getStoreMetrics.getCurrentApplicationName(strForeGroundTask));
        frgrdAppName.setIsAttentionRequired(0);

        DeviceMetric wifiName = getWifiName();
        wifiName.setDeviceMetricValue(getStoreMetrics.getCurrentSsid());
        wifiName.setIsAttentionRequired(0);

        DeviceMetric deviceIP = getDeviceIP();
        deviceIP.setDeviceMetricValue(getStoreMetrics.getIpAddress());
        deviceIP.setIsAttentionRequired(0);

        DeviceMetric deviceGatewayMetric = getDeviceGatewayMetric();
        deviceGatewayMetric.setDeviceMetricValue(getStoreMetrics.getDeviceGatewayValue());
        deviceGatewayMetric.setIsAttentionRequired(0);

        DeviceMetric deviceSerial = getDeviceSerial();
        deviceSerial.setDeviceMetricValue(GetStoreMetrics.getSerialNumber());
        deviceSerial.setIsAttentionRequired(0);

        DeviceMetric screenState = getScreenStateMetric();
        screenState.setDeviceMetricValue(getStoreMetrics.getScreenStateValue());
        screenState.setIsAttentionRequired(0);

        DeviceMetric screenBrightness = getScreenBrightness();
        screenBrightness.setDeviceMetricValue(getStoreMetrics.getScreenBrighness());
        screenBrightness.setIsAttentionRequired(0);

        DeviceMetric foreGroundAppVersion = getForeGroundAppVersion();
        foreGroundAppVersion.setDeviceMetricValue(getStoreMetrics.getForegroundApplicationVersion(strForeGroundTask));
        foreGroundAppVersion.setIsAttentionRequired(0);

        DeviceMetric cpuInfo = getCpuInfo();
        cpuInfo.setDeviceMetricValue(String.valueOf(getCpuUsageStatistic()) + " %");

        DeviceMetric ramInfo = getRamInfo();
        MemorySize memorySize = getMemorySize();
        avlMem = memorySize.free/1048576;
        totMem = memorySize.total/1048576;
        usedMem = totMem - avlMem;
        ramInfo.setDeviceMetricValue(usedMem + " MB out of " + totMem + " MB");

        DeviceMetric ramPerc = getRamPerc();
        DecimalFormat df = new DecimalFormat("##.##");
        if (totMem > 0)
        {
            ramPerc.setDeviceMetricValue(df.format(((float) (totMem - avlMem) / (float) totMem) * 100) + " %");
        }


        hasInitialized = true;
    }




    private static MemorySize getMemorySize()
    {
        final Pattern PATTERN = Pattern.compile("([a-zA-Z]+):\\s*(\\d+)");

        MemorySize result = new MemorySize();
        String line;
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
            while ((line = reader.readLine()) != null) {
                Matcher m = PATTERN.matcher(line);
                if (m.find()) {
                    String name = m.group(1);
                    String size = m.group(2);

                    if (name.equalsIgnoreCase("MemTotal")) {
                        result.total = Long.parseLong(size);
                    } else if (name.equalsIgnoreCase("MemFree") || name.equalsIgnoreCase("Buffers") ||
                            name.equalsIgnoreCase("Cached") || name.equalsIgnoreCase("SwapFree")) {
                        result.free += Long.parseLong(size);
                    }
                }
            }
            reader.close();

            result.total *= 1024;
            result.free *= 1024;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static class MemorySize {
        public long total = 0;
        public long free = 0;
    }

    private float readUsage()
    {
        try
        {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();

            String[] toks = load.split(" +");

            long work1 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
            long total1 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4]) + Long.parseLong(toks[5]) + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            try
            {
                Thread.sleep(360);
            }
            catch (Exception e)
            {
            }

            reader.seek(0);
            load = reader.readLine();
            reader.close();


            toks = load.split(" +");


            long work2 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
            long total2 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4]) + Long.parseLong(toks[5]) + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            return (float) (work2 - work1) / (total2 - total1);


        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int getCpuUsageStatistic()
    {

        String tempString = executeTop();
        int totalUsage=0;

        tempString = tempString.replaceAll(",", "");
        tempString = tempString.replaceAll("User", "");
        tempString = tempString.replaceAll("System", "");
        tempString = tempString.replaceAll("IOW", "");
        tempString = tempString.replaceAll("IRQ", "");
        tempString = tempString.replaceAll("%", "");
        for (int i = 0; i < 10; i++) {
            tempString = tempString.replaceAll("  ", " ");
        }
        tempString = tempString.trim();
        String[] myString = tempString.split(" ");
        int[] cpuUsageAsInt = new int[myString.length];
        for (int i = 0; i < myString.length; i++) {
            myString[i] = myString[i].trim();
            cpuUsageAsInt[i] = Integer.parseInt(myString[i]);
            totalUsage+=cpuUsageAsInt[i];
        }
        return totalUsage;
    }

    private static String executeTop() {
        java.lang.Process p = null;
        BufferedReader in = null;
        String returnString = null;
        try {
            p = Runtime.getRuntime().exec("top -n 1");
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (returnString == null || returnString.contentEquals("")) {
                returnString = in.readLine();
            }
        } catch (IOException e) {
            Log.e("executeTop", "error in getting first line of top");
            e.printStackTrace();
        } finally {
            try {
                in.close();
                p.destroy();
            } catch (IOException e) {
                Log.e("executeTop",
                        "error in closing and destroying top process");
                e.printStackTrace();
            }
        }
        return returnString;
    }
}
