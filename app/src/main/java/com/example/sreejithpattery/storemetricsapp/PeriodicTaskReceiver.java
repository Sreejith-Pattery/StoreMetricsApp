package com.example.sreejithpattery.storemetricsapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by 310722 on 4/22/2016.
 */
public class PeriodicTaskReceiver extends BroadcastReceiver
{
    private static final String TAG = "Periodic Task Receiver";
    private static final String INTENT_ACTION = "com.example.sreejithpattery.myserviceexample.PERIODIC_TASK_HEART_BEAT";
    private static final String BROADCAST_RESULT = "com.example.sreejithpattery.myserviceexample.service_processed";
    private static final String BROADCAST_MESSAGE = "com.example.sreejithpattery.myserviceexample.broadcast_message";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(!intent.getAction().isEmpty()&& !(intent.getAction()==null))
        {
            MyApplication myApplication = (MyApplication) context.getApplicationContext();
            SharedPreferences sharedPreferences = myApplication.getSharedPreferences("MySharedPreferences",Context.MODE_PRIVATE);

            if(intent.getAction().equals("android.intent.action.BATTERY_LOW"))
            {
                sharedPreferences.edit().putBoolean("BACKGROUNDSERVICE_BATTERY_CONTROL",false).apply();
                stopPeriodicTaskHeartBeat(context);
            }
            else if(intent.getAction().equals("android.intent.action.BATTERY_OK"))
            {
                sharedPreferences.edit().putBoolean("BACKGROUND_SERVICE_BATTERY_CONTROL",true).apply();
                restartPeriodicTaskHeartBeat(context,myApplication);
            }
            else if(intent.getAction().equals(INTENT_ACTION))
            {
                doPeriodicTask(context,myApplication);
            }

        }

    }

    private void doPeriodicTask(Context context, MyApplication myApplication)
    {
        Toast.makeText(context,"Period task in progress",Toast.LENGTH_SHORT).show();

        sendUiUpdateBroadcast(context);

    }

    private void sendUiUpdateBroadcast(Context context)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String alarmTime = dateFormat.format(new Date());
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);

        Intent intent = new Intent(BROADCAST_RESULT);
        intent.putExtra(BROADCAST_MESSAGE,alarmTime);

        localBroadcastManager.sendBroadcast(intent);

    }

    public void restartPeriodicTaskHeartBeat(Context context, MyApplication myApplication)
    {
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences("MySharedPreferences",Context.MODE_PRIVATE);
        boolean isBatteryOK = sharedPreferences.getBoolean("BACKGROUND_SERVICE_BATTERY_CONTROL", true);
        Intent alarmIntent = new Intent(context,PeriodicTaskReceiver.class);
        boolean isAlarmUp = PendingIntent.getBroadcast(context,0, alarmIntent,0)!=null;

        if(isBatteryOK && isAlarmUp)
        {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmIntent.setAction(INTENT_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,alarmIntent,0);
            //alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),AlarmManager.INTERVAL_FIFTEEN_MINUTES,pendingIntent);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),60000L,pendingIntent);
        }
    }

    public void stopPeriodicTaskHeartBeat(Context context)
    {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context,PeriodicTaskReceiver.class);
        alarmIntent.setAction(INTENT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,alarmIntent,0);
        alarmManager.cancel(pendingIntent);
    }




}
