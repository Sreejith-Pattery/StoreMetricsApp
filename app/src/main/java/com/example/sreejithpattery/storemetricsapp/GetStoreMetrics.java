package com.example.sreejithpattery.storemetricsapp;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.Display;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by 310722 on 3/30/2016.
 */
public class GetStoreMetrics
{
    private static Context context;

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context context) {
        GetStoreMetrics.context = context;
    }

    public GetStoreMetrics(Context context)
    {
        setContext(context);
    }

    public static boolean externalMemoryAvailable()
    {
        return android.os.Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static String getAvailableInternalMemorySize()
    {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long BlockSize = stat.getBlockSizeLong();
        long avilableBlocks= stat.getAvailableBlocks();
        return formatSize(avilableBlocks * BlockSize);
    }


    public static DeviceMetric getAvailableInternalMemorySizePerc(DeviceMetric memoryMetric)
    {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long BlockSize = stat.getBlockSizeLong();
        long avilableBlocks= stat.getAvailableBlocks();
        long totalBlocks= stat.getBlockCountLong();
        float availableMemPerc = (float)((avilableBlocks * BlockSize)*100/(totalBlocks * BlockSize));
        memoryMetric.setDeviceMetricValue(availableMemPerc+" %");
        if(availableMemPerc<30)
            memoryMetric.setIsAttentionRequired(1);
        else
            memoryMetric.setIsAttentionRequired(0);
        //return availableMemPerc+" %";
        return memoryMetric;
    }

    public static String getTotalInternalMemorySize()
    {

        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long BlockSize = stat.getBlockSizeLong();
        long totalBlocks= stat.getBlockCountLong();
        return formatSize(totalBlocks * BlockSize);
    }

    public static String getAvailableExternalMemorySize()
    {
        if(externalMemoryAvailable())
        {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long BlockSize = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocks();
            return formatSize(availableBlocks*BlockSize);
        }
        else
            return "ERROR ";
    }

    public static String getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return formatSize(totalBlocks * blockSize);
        } else {
            return "ERROR";
        }
    }


    public static String formatSize(long size) {
        String suffix = null;

        if (size >= 1024) {
            suffix = " KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = " MB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }






    public static String getSerialNumber()
    {

        if(Build.SERIAL!=null)
            return Build.SERIAL;
        else
            return "ERROR";
    }



    /*
    // get hardware serial number
     public String getHardwareSerialNumber()
     {
         Class<?> c = Class.forName("android.os.SystemProperties");
        Method get = c.getMethod("get", String.class, String.class);
        serialNumber = (String) get.invoke(c, "sys.serialnumber", "Error");
        if(serialNumber.equals("Error")) {
            serialNumber = (String) get.invoke(c, "ril.serialnumber", "Error");
        }
     }

     */

    public String getScreenBrighness()
    {
        String screenBrightness = "ERROR";
        float curBrightnessValue=0;

//        curBrightnessValue =  ((MainActivity)getContext()).getWindow().getAttributes().screenBrightness;
//
//        if(curBrightnessValue<0)
//        {
            try
            {
                curBrightnessValue = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                screenBrightness = String.valueOf(curBrightnessValue)+" out of 255";
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
//        }
//        else
//        {
//            screenBrightness = String.valueOf(curBrightnessValue)+" out of 1";
//        }

        return screenBrightness;
    }

    public String getForegroundTask()
    {
        String currentApp = "NULL";

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            UsageStatsManager usm = (UsageStatsManager) getContext().getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR,-1);

            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,cal.getTimeInMillis(),System.currentTimeMillis());
            if(appList!=null)
            {
                if(appList.size()== 0)
                {
                    Toast.makeText(context, "Enable Usage settings for Store Metrics app, if not enabled", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
                else
                {
                    SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
                    for (UsageStats usagestat : appList
                            ) {
                        sortedMap.put(usagestat.getLastTimeUsed(), usagestat);
                    }

                    if (sortedMap != null && !sortedMap.isEmpty()) {
                        currentApp = sortedMap.get(sortedMap.lastKey()).getPackageName();
                    }
                }
            }
        }
        else
        {
            ActivityManager am = (ActivityManager)  getContext().getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> listProcesses= am.getRunningAppProcesses();
            if(!listProcesses.isEmpty())
            {
                currentApp = listProcesses.get(0).processName;
            }
        }

        return currentApp;
    }

    private WifiInfo getWifiInfo() {
        final WifiManager wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        return wifiManager.getConnectionInfo();
    }

    private NetworkInfo getNetworkInfo() {
        ConnectivityManager connManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

    public DeviceMetric getCurrentSignalStrength(DeviceMetric signalMetric)
    {
        int level = -1;
        NetworkInfo networkInfo = getNetworkInfo();
        if(networkInfo.isConnected())
        {
            final WifiInfo wifiInfo = getWifiInfo();
            level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
        }

        signalMetric.setDeviceMetricValue(level+" out of 5");

        if(level<2)
            signalMetric.setIsAttentionRequired(4);
        else
            signalMetric.setIsAttentionRequired(0);

        return signalMetric;
    }

    public String getIpAddress()
    {
        String ipAddress = "ERROR";
        NetworkInfo networkInfo = getNetworkInfo();
        if(networkInfo.isConnected())
        {
            final WifiInfo wifiInfo = getWifiInfo();
            ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
        }
        return ipAddress;
    }

    @SuppressWarnings("deprecation")
    public String getDHCPServer()
    {
        String dhcp_ipaddress = "ERROR";
        NetworkInfo networkInfo = getNetworkInfo();
        if(networkInfo.isConnected())
        {
            final WifiManager wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            dhcp_ipaddress = Formatter.formatIpAddress(dhcpInfo.serverAddress);
        }

        return dhcp_ipaddress;
    }

    @SuppressWarnings("deprecation")
    public String getDeviceGatewayValue()
    {
        String dhcp_gateway = "ERROR";
        NetworkInfo networkInfo = getNetworkInfo();
        if(networkInfo.isConnected())
        {
            final WifiManager wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            dhcp_gateway = Formatter.formatIpAddress(dhcpInfo.gateway);
        }

        return dhcp_gateway;

    }


    public String getCurrentSsid()
    {
        String ssid = "ERROR";
        NetworkInfo networkInfo = getNetworkInfo();
        if(networkInfo.isConnected())
        {
            final WifiInfo wifiInfo = getWifiInfo();
            if(wifiInfo!=null && !TextUtils.isEmpty(wifiInfo.getSSID()))
            {
                ssid = wifiInfo.getSSID();
            }
        }
        return ssid;
    }



    public String getCurrentApplicationName(String packageName)
    {
        PackageManager packageManager = getContext().getPackageManager();
        String appName = "ERROR";
        try
        {
            return (String) packageManager.getApplicationLabel((ApplicationInfo)packageManager.getApplicationInfo(packageName,0));
        } catch (PackageManager.NameNotFoundException e)
        {

            e.printStackTrace();
        }
        return appName;
    }

    public String getScreenStateValue()
    {
        String screenState = "ERROR";
        if(Build.VERSION.SDK_INT >= 20)
        {
            DisplayManager displayManager = (DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE);
            for(Display display:displayManager.getDisplays())
            {
                if(display.getState()==Display.STATE_ON || display.getState()== Display.STATE_UNKNOWN)
                {
                    screenState = "Screen is ON";
                }
                else
                {
                    screenState = "Screen is OFF";
                }
            }
        }
        else
        {
            PowerManager powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            if(powerManager.isScreenOn())
            {
                screenState = "Screen is ON";
            }
            else
            {
                screenState = "Screen is OFF";
            }
        }

        return screenState;
    }


    public String getForegroundApplicationVersion(String packageName)
    {
        PackageManager packageManager = getContext().getApplicationContext().getPackageManager();
        String appName = "ERROR";
        try
        {
            return (String) packageManager.getPackageInfo(packageName,0).versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {

            e.printStackTrace();
        }
        return appName;

    }

    private float readUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();

            String[] toks = load.split(" +");

            long work1 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
            long total1 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4]) + Long.parseLong(toks[5]) + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            try {
                Thread.sleep(360);
            } catch (Exception e) {
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




}
