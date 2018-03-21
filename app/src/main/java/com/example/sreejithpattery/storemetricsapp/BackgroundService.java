package com.example.sreejithpattery.storemetricsapp;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by 310722 on 4/25/2016.
 */
public class BackgroundService extends Service
{
    private static final String TAG = "BackgroundService";

    PeriodicTaskReceiver periodicTaskReceiver = new PeriodicTaskReceiver();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        MyApplication myApplication = (MyApplication) getApplicationContext();
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences("MySharedPreferences", Context.MODE_PRIVATE);
        IntentFilter batteryStatusIntentFilter  = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntend = registerReceiver(null,batteryStatusIntentFilter);

        if(batteryStatusIntend!=null)
        {
            int level = batteryStatusIntend.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            int scale = batteryStatusIntend.getIntExtra(BatteryManager.EXTRA_SCALE,-1);

            float batteryPercentage = level / (float) scale;
            float lowBatteryPercentageLevel = 0.14f;

            try
            {
                int lowBatteryLevel = Resources.getSystem().getInteger(Resources.getSystem().getIdentifier("config_lowBatteryWarningLevel","integer","android"));
                lowBatteryPercentageLevel = lowBatteryLevel/(float) scale;
            }
            catch (Exception ex)
            {
                Log.e(TAG, "Missing low battery threshold resource");
            }

            sharedPreferences.edit().putBoolean("BACKGROUND_SERVICE_BATTERY_CONTROL",batteryPercentage>=lowBatteryPercentageLevel).apply();
        }
        else
        {
            sharedPreferences.edit().putBoolean("BACKGROUND_SERVICE_BATTERY_CONTROL",true);
        }

        periodicTaskReceiver.restartPeriodicTaskHeartBeat(BackgroundService.this,myApplication);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
