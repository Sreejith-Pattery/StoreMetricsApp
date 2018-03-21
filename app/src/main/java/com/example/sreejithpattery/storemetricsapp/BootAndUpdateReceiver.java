package com.example.sreejithpattery.storemetricsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by 310722 on 4/25/2016.
 */
public class BootAndUpdateReceiver extends BroadcastReceiver
{

    private static final String TAG = "BootAndUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")||intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED"))
        {
            Intent startServiceIntent = new Intent(context,BackgroundService.class);
            context.startService(startServiceIntent);
        }
    }
}
