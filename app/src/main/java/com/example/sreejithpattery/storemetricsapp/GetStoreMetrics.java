package com.example.sreejithpattery.storemetricsapp;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
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

import java.io.File;
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


    public static String getAvailableInternalMemorySizePerc()
    {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long BlockSize = stat.getBlockSizeLong();
        long avilableBlocks= stat.getAvailableBlocks();
        long totalBlocks= stat.getBlockCountLong();
        float availableMemPerc = (float)((avilableBlocks * BlockSize)*100/(totalBlocks * BlockSize));
        return availableMemPerc+" %";
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



}
