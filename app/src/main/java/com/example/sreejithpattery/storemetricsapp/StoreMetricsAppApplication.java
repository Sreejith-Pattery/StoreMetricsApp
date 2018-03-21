package com.example.sreejithpattery.storemetricsapp;

import android.app.Application;
import android.content.Intent;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by 310722 on 4/7/2016.
 */
public class StoreMetricsAppApplication extends Application
{
    private Tracker mTracker;
    private static StoreMetricsAppApplication mInstance;
    public static StoreMetricsContainer storeMetricsContainer;

    public StoreMetricsAppApplication()
    {
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        mInstance = this;
        storeMetricsContainer = new StoreMetricsContainer(getBaseContext());

        Intent startServiceIntent = new Intent(getBaseContext(),BackgroundService.class);
        startService(startServiceIntent);
    }

    public static StoreMetricsAppApplication getmInstance()
    {
        return mInstance;
    }

    synchronized public Tracker getDefaultTracker()
    {
        if(mTracker == null)
        {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }

        return mTracker;
    }
}
