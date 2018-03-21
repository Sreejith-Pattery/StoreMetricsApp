package com.example.sreejithpattery.storemetricsapp;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by 310722 on 4/25/2016.
 */
public class BackgroundService extends Service implements SensorEventListener,
        com.google.android.gms.location.LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{


    private static final String TAG = "BackgroundService";
    private static final int MY_PERMISSIONS_REQUEST_READ_FINE_LOCATION = 100;

    PeriodicTaskReceiver periodicTaskReceiver = new PeriodicTaskReceiver();
    BatteryMetricsReceiver batteryMetricsReceiver = new BatteryMetricsReceiver();
    public Tracker mTracker;
    GoogleAnalytics analytics;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude;
    private double currentLongitude;
    float[] mGravity;
    float mAccelLast = 0;
    float mAccelCurrent = 0;
    float mAccel = 0;
    private SensorManager sensorManager;
    private Sensor accelorometer;
    private LocationManager locationManager;
    private static final String BROADCAST_RESULT = "com.example.sreejithpattery.storemetricsapp.service_processed";
    private static final String BROADCAST_MESSAGE = "com.example.sreejithpattery.storemetricsapp.broadcast_message";
    private BroadcastReceiver receiver;
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 1; //

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        StoreMetricsAppApplication myApplication = (StoreMetricsAppApplication) getApplicationContext();
        SharedPreferences sharedPreferences = myApplication.getSharedPreferences("MySharedPreferences", Context.MODE_PRIVATE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelorometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager.registerListener(this, accelorometer, SensorManager.SENSOR_DELAY_UI);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(MIN_TIME_BW_UPDATES)
                .setFastestInterval(1 * 1000);


        mGoogleApiClient.connect();

        StoreMetricsAppApplication application = (StoreMetricsAppApplication) getApplication();
        mTracker = application.getDefaultTracker();
        analytics = GoogleAnalytics.getInstance(this);

        IntentFilter batteryStatusIntentFilter  = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntend = registerReceiver(null,batteryStatusIntentFilter);
        registerReceiver(batteryMetricsReceiver,batteryStatusIntentFilter);

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


        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String s = intent.getStringExtra(BROADCAST_MESSAGE);
                sendDeviceMetricsToGA();
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,new IntentFilter(BROADCAST_RESULT));


        periodicTaskReceiver.restartPeriodicTaskHeartBeat(BackgroundService.this,myApplication);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(location == null)
        {
            if(mGoogleApiClient.isConnected())
            {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
//            Toast.makeText(this, "Please enable Location services, if not enabled already", Toast.LENGTH_SHORT).show();
//            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
        else
        {
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
            DeviceMetric metricLat = StoreMetricsContainer.getLatitude();
            DeviceMetric metricLng = StoreMetricsContainer.getLongitude();
            metricLat.setDeviceMetricName("Latitude");
            metricLat.setDeviceMetricValue(String.valueOf(currentLatitude));
            if(currentLongitude<=0)
                metricLat.setIsAttentionRequired(1);
            else
                metricLat.setIsAttentionRequired(0);

            metricLng.setDeviceMetricName("Longitude");
            metricLng.setDeviceMetricValue(String.valueOf(currentLongitude));
            if(currentLongitude<=0)
                metricLng.setIsAttentionRequired(1);
            else
                metricLng.setIsAttentionRequired(0);

        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG,"Location Services failed with the code"+connectionResult.getErrorCode());

        if(connectionResult.getErrorCode() == ConnectionResult.SERVICE_MISSING)
        {
            Log.e(TAG, "Location Services not available on this device");
        }
    }


    @Override
    public void onLocationChanged(Location location)
    {
        if(location!=null)
        {
            DeviceMetric latMetric = StoreMetricsContainer.getLatitude();
            DeviceMetric lngMetric = StoreMetricsContainer.getLongitude();

            latMetric.setDeviceMetricValue(String.valueOf(location.getLatitude()));
            lngMetric.setDeviceMetricValue(String.valueOf(location.getLongitude()));

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        DeviceMetric isMovingMetric = StoreMetricsContainer.getIsMoving();

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            mGravity = event.values.clone();
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];

            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = mAccelLast - mAccelCurrent;
            mAccel = mAccel*0.9f+delta;

            if(mAccel>1)
            {
                isMovingMetric.setDeviceMetricValue("MOVING");
                isMovingMetric.setIsAttentionRequired(0);
            }
            else
            {
                isMovingMetric.setDeviceMetricValue("NOT MOVING");
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        sensorManager.unregisterListener(this);

        //Disconnect from API onPause()
        if (mGoogleApiClient.isConnected())
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private void sendDeviceMetricsToGA()
    {
        //Toast.makeText(this,"Posting metrics to Google Analytics",Toast.LENGTH_LONG).show();
        String strDeviceSerialNumber = GetStoreMetrics.getSerialNumber();

        mTracker.setScreenName("Device Metrics");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getDeviceStrgPerc()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getDeviceStrgPerc()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getDeviceStrgPerc()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getDeviceStrgMB()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getDeviceStrgMB()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getDeviceStrgMB()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getFrgrdAppID()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getFrgrdAppID()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getFrgrdAppID()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getFrgrdAppName()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getFrgrdAppName()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getFrgrdAppName()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getWifiName()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getWifiName()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getWifiName()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getWifiStrngth()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getWifiStrngth()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getWifiStrngth()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getDeviceIP()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getDeviceIP()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getDeviceIP()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getDeviceGatewayMetric()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getDeviceGatewayMetric()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getDeviceGatewayMetric()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getCpuInfo()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getCpuInfo()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getCpuInfo()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getRamInfo()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getRamInfo()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getRamInfo()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getRamPerc()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getRamPerc()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getRamPerc()).isAttentionRequired())
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getIsMoving()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getIsMoving()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getIsMoving()).isAttentionRequired())
                .build());

        if(!Build.MODEL.equals("MC40N0"))
        {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getLatitude()).getDeviceMetricName())
                    .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getLatitude()).getDeviceMetricValue())
                    .setCustomDimension(3, strDeviceSerialNumber)
                    .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getLatitude()).isAttentionRequired())
                    .build());

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getLongitude()).getDeviceMetricName())
                    .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getLongitude()).getDeviceMetricValue())
                    .setCustomDimension(3, strDeviceSerialNumber)
                    .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getLongitude()).isAttentionRequired())
                    .build());
        }


        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getBatteryHealth()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getBatteryHealth()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getBatteryHealth()).isAttentionRequired)
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getBatteryChargeLvl()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getBatteryChargeLvl()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getBatteryChargeLvl()).isAttentionRequired)
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getBatteryTmp()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getBatteryTmp()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getBatteryTmp()).isAttentionRequired)
                .build());

        mTracker.send(new HitBuilders.EventBuilder()
                .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getBatteryChrgSts()).getDeviceMetricName())
                .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getBatteryChrgSts()).getDeviceMetricValue())
                .setCustomDimension(3, strDeviceSerialNumber)
                .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getBatteryChrgSts()).isAttentionRequired)
                .build());

        if(Build.MODEL.equals("MC40N0"))
        {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getBatteryMfd()).getDeviceMetricName())
                    .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getBatteryMfd()).getDeviceMetricValue())
                    .setCustomDimension(3, strDeviceSerialNumber)
                    .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getBatteryMfd()).isAttentionRequired)
                    .build());

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCustomDimension(1, ((DeviceMetric) StoreMetricsContainer.getBatterySerial()).getDeviceMetricName())
                    .setCustomDimension(2, ((DeviceMetric) StoreMetricsContainer.getBatterySerial()).getDeviceMetricValue())
                    .setCustomDimension(3, strDeviceSerialNumber)
                    .setCustomMetric(1, ((DeviceMetric) StoreMetricsContainer.getBatterySerial()).isAttentionRequired)
                    .build());
        }

        analytics.dispatchLocalHits();
    }

}
