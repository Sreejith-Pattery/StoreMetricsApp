package com.example.sreejithpattery.storemetricsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by 310722 on 4/26/2016.
 */
public class BatteryMetricsReceiver extends BroadcastReceiver
{

    private static final String BATTERYCHANGED_BROADCAST = "com.example.sreejithpattery.storemetricsapp.batterychange_broadcast";
    private static final String BATTERYCHANGED_MESSAGE = "com.example.sreejithpattery.storemetricsapp.batterychange_message";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
//            int icon_small = intent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean present = intent.getExtras().getBoolean(BatteryManager.EXTRA_PRESENT);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
        String technology = intent.getExtras().getString(BatteryManager.EXTRA_TECHNOLOGY);
        int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        float fltemperature = temperature / 10;
        int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);


        String strHealth = "ERROR";
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_DEAD:
                strHealth = "BATTERY HEALTH DEAD";
                break;
            case BatteryManager.BATTERY_HEALTH_COLD:
                strHealth = "BATTERY HEALTH COLD";
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                strHealth = "BATTERY HEALTH GOOD";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                strHealth = "BATTERY HEALTH OVERVOLTAGE";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                strHealth = "BATTERY HEALTH OVERHEAT";
                break;
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                strHealth = "BATTERY HEALTH UNKNOWN";
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                strHealth = "BATTERY HEALTH UNSPECIFIED FAILURE";
                break;
            default:
                strHealth = "ERROR";
        }

        String strBatteryPlugged = "ERROR";
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                strBatteryPlugged = "BATTERY PLUGGED AC";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                strBatteryPlugged = "BATTERY PLUGGED USB";
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                strBatteryPlugged = "BATTERY PLUGGED WIRELESS";
                break;
            case 0:
                strBatteryPlugged = "BATTERY DISCHARGING";
                break;
            default:
                strBatteryPlugged = "ERROR";
                break;
        }


        DeviceMetric batteryHealth = StoreMetricsContainer.getBatteryHealth();
        batteryHealth.setDeviceMetricValue(strHealth);

        if (!strHealth.equals("BATTERY HEALTH GOOD"))
            batteryHealth.setIsAttentionRequired(1);
        else
            batteryHealth.setIsAttentionRequired(0);

        DeviceMetric batteryChargeLvl = StoreMetricsContainer.getBatteryChargeLvl();
        batteryChargeLvl.setDeviceMetricValue(level + " out of " + scale);

        if (level < 100)
            batteryChargeLvl.setIsAttentionRequired(1);
        else
            batteryChargeLvl.setIsAttentionRequired(0);


        DeviceMetric batteryTmp = StoreMetricsContainer.getBatteryTmp();
        batteryTmp.setDeviceMetricValue(fltemperature + " Degrees Celsius");

        if (fltemperature > 30)
            batteryTmp.setIsAttentionRequired(1);
        else
            batteryTmp.setIsAttentionRequired(0);


        DeviceMetric batteryChrgSts = StoreMetricsContainer.getBatteryChrgSts();
        batteryChrgSts.setDeviceMetricValue(strBatteryPlugged);
        batteryChrgSts.setIsAttentionRequired(0);

        StoreMetricsContainer.batteryHealth = batteryHealth;
        StoreMetricsContainer.batteryChargeLvl = batteryChargeLvl;
        StoreMetricsContainer.batteryTmp = batteryTmp;
        StoreMetricsContainer.batteryChrgSts = batteryChrgSts;

        DeviceMetric batteryMfd = StoreMetricsContainer.getBatteryMfd();
        DeviceMetric batterySerial = StoreMetricsContainer.getBatterySerial();
        batterySerial.setIsAttentionRequired(0);

        String serialnumber = "ERROR";
        String mfd = "ERROR";
        int cycle = 0;

        if (Build.MODEL.equals("MC40N0")) {

            mfd = intent.getExtras().getString("mfd");
            batteryMfd.setDeviceMetricValue(mfd);

            SimpleDateFormat date = new SimpleDateFormat("yyyy-mm-dd");

            Date dtMFd = null;
            try {
                dtMFd = date.parse(mfd);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Date dtToday = new Date();
            float diff = (Math.abs(dtToday.getTime() - dtMFd.getTime()) / (1000 * 60 * 60 * 24)) / 365;

            if (diff < 1)
                batteryMfd.setIsAttentionRequired(0);
            else
                batteryMfd.setIsAttentionRequired(1);

            serialnumber = intent.getExtras().getString("serialnumber");
            batterySerial.setDeviceMetricValue(serialnumber);
            batterySerial.setIsAttentionRequired(1);

            cycle = intent.getExtras().getInt("cycle");

            StoreMetricsContainer.batteryMfd = batteryMfd;
            StoreMetricsContainer.batterySerial = batterySerial;

        }

        sendBatteryUpdateBroadcast(context);
    }


    private void sendBatteryUpdateBroadcast(Context context)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String alarmTime = dateFormat.format(new Date());
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);

        Intent intent = new Intent(BATTERYCHANGED_BROADCAST);
        intent.putExtra(BATTERYCHANGED_MESSAGE,alarmTime);

        localBroadcastManager.sendBroadcast(intent);

    }


}
