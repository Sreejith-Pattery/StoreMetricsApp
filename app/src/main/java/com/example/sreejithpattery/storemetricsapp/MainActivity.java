package com.example.sreejithpattery.storemetricsapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements SensorEventListener,
        com.google.android.gms.location.LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    private List<DeviceMetric> deviceMetrics = new ArrayList<>();
    private List<DeviceMetric> batteryMetrics = new ArrayList<>();
    private List<DeviceMetric> otherMetrics = new ArrayList<>();


    GetStoreMetrics getStoreMetrics;
    private long avlMem;
    private long totMem;
    private long usedMem;

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
    private BroadcastReceiver receiver;
    private BroadcastReceiver batteryReceiver;
    private static final String BROADCAST_RESULT = "com.example.sreejithpattery.storemetricsapp.service_processed";
    private static final String BROADCAST_MESSAGE = "com.example.sreejithpattery.storemetricsapp.broadcast_message";
    private static final String BATTERYCHANGED_BROADCAST = "com.example.sreejithpattery.storemetricsapp.batterychange_broadcast";
    private static final String BATTERYCHANGED_MESSAGE = "com.example.sreejithpattery.storemetricsapp.batterychange_message";
    private ArrayAdapter<DeviceMetric> deviceListAdapter;
    private ArrayAdapter<DeviceMetric> batteryListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelorometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

//        getCurrentLocation();

        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("Device Metrics");
        tabSpec.setContent(R.id.tab1);
        tabSpec.setIndicator("DEVICE\nMETRICS");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("Battery Metrics");
        tabSpec.setContent(R.id.tab2);
        tabSpec.setIndicator("BATTERY\nMETRICS");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("Location Metrics");
        tabSpec.setContent(R.id.tab3);
        tabSpec.setIndicator("OTHER\nMETRICS");
        tabHost.addTab(tabSpec);

        getStoreMetrics = new GetStoreMetrics(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10*1000)
                .setFastestInterval(1*1000);

        StoreMetricsAppApplication application = (StoreMetricsAppApplication) getApplication();
        mTracker = application.getDefaultTracker();
        analytics = GoogleAnalytics.getInstance(this);

        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String s = intent.getStringExtra(BROADCAST_MESSAGE);
                if(deviceListAdapter!=null)
                {
                    setMainDeviceMetrics();
                    deviceListAdapter.notifyDataSetChanged();
                }
            }
        };

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(batteryListAdapter!=null)
                {
                    setBatteryMetrics();
                    batteryListAdapter.notifyDataSetChanged();
                }
            }
        };

    }

    private void loadOtherMetrics()
    {
        otherListAdapter = new OtherMetricsAdapter(this, R.layout.item_view,otherMetrics);
        ListView lstOtherMetrics = (ListView) findViewById(R.id.lstOtherMetrics);
        lstOtherMetrics.setAdapter(otherListAdapter);
    }


    private void loadDeviceMetrics()
    {
        deviceListAdapter = new DeviceListAdapter(this, R.layout.item_view,deviceMetrics);
        ListView lstDeviceMetrics = (ListView) findViewById(R.id.lstDeviceMetrics);
        lstDeviceMetrics.setAdapter(deviceListAdapter);
    }

    private void loadBatteryMetrics()
    {
        batteryListAdapter = new OtherMetricsAdapter(this,R.layout.item_view,batteryMetrics);
        ListView lstDeviceMetrics = (ListView) findViewById(R.id.lstBatteryMetrics);
        lstDeviceMetrics.setAdapter(batteryListAdapter);
    }

    private void setDeviceMetrics()
    {
        StoreMetricsContainer.setDeviceMetrics();

        setMainDeviceMetrics();

        setBatteryMetrics();

        otherMetrics.clear();
        otherMetrics.add(StoreMetricsContainer.getCpuInfo());
        otherMetrics.add(StoreMetricsContainer.getRamInfo());
        otherMetrics.add(StoreMetricsContainer.getRamPerc());
        otherMetrics.add(StoreMetricsContainer.getIsMoving());
        otherMetrics.add(StoreMetricsContainer.getLatitude());
        otherMetrics.add(StoreMetricsContainer.getLongitude());
    }

    private void setMainDeviceMetrics() {
        deviceMetrics.clear();
        deviceMetrics.add(StoreMetricsContainer.getDeviceStrgPerc());
        deviceMetrics.add(StoreMetricsContainer.getDeviceStrgMB());
        deviceMetrics.add(StoreMetricsContainer.getFrgrdAppID());
        deviceMetrics.add(StoreMetricsContainer.getFrgrdAppName());
        deviceMetrics.add(StoreMetricsContainer.getWifiName());
        deviceMetrics.add(StoreMetricsContainer.getWifiStrngth());
        deviceMetrics.add(StoreMetricsContainer.getDeviceIP());
        deviceMetrics.add(StoreMetricsContainer.getDeviceGatewayMetric());
        deviceMetrics.add(StoreMetricsContainer.getDeviceSerial());
        deviceMetrics.add(StoreMetricsContainer.getScreenStateMetric());
        deviceMetrics.add(StoreMetricsContainer.getScreenBrightness());
        deviceMetrics.add(StoreMetricsContainer.getForeGroundAppVersion());
    }

    private void setBatteryMetrics() {
        if(batteryMetrics.size()>0)
            batteryMetrics.clear();

        batteryMetrics.add(StoreMetricsContainer.getBatteryHealth());
        batteryMetrics.add(StoreMetricsContainer.getBatteryChargeLvl());
        batteryMetrics.add(StoreMetricsContainer.getBatteryTmp());
        batteryMetrics.add(StoreMetricsContainer.getBatteryChrgSts());
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

    private MemorySize getMemorySize() {
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



    private void startRAMCPUThread()
    {
        handler = new Handler();
        r = new Runnable() {
            @Override
            public void run()
            {
                if(shouldThreadRun)
                {
                    /*
                    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                    ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);


                    activityManager.getMemoryInfo(memoryInfo);
                    avlMem = memoryInfo.availMem / 1048576;
                    totMem = memoryInfo.totalMem / 1048576;
                    usedMem = ((memoryInfo.totalMem - memoryInfo.availMem)) / 1048576;
                     */

                    MemorySize memorySize = getMemorySize();

                    avlMem = memorySize.free/1048576;
                    totMem = memorySize.total/1048576;
                    usedMem = totMem - avlMem;

                    DeviceMetric cpuInfo = StoreMetricsContainer.getCpuInfo();
                    DeviceMetric ramInfo = StoreMetricsContainer.getRamInfo();
                    DeviceMetric ramPerc = StoreMetricsContainer.getRamPerc();

                    //cpuInfo.setDeviceMetricValue(String.valueOf(readUsage() * 100) + " %");
                    cpuInfo.setDeviceMetricValue(String.valueOf(getCpuUsageStatistic()) + " %");

                    ramInfo.setDeviceMetricValue(usedMem + " MB out of " + totMem + " MB");


                    DecimalFormat df = new DecimalFormat("##.##");

                    if (totMem > 0) {
                        ramPerc.setDeviceMetricValue(df.format(((float) (totMem - avlMem) / (float) totMem) * 100) + " %");
                    }

                    //setCpuInfo(cpuInfo);
                    //setRamInfo(ramInfo);
                    //setRamPerc(ramPerc);

                    otherListAdapter.notifyDataSetChanged();

                    handler.postDelayed(this, 1000);


                }
            }

        };

        handler.postDelayed(r, 1000);

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

            //setIsMoving(isMovingMetric);

            otherListAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(BROADCAST_RESULT));
        LocalBroadcastManager.getInstance(this).registerReceiver(batteryReceiver, new IntentFilter(BATTERYCHANGED_BROADCAST));
    }

    @Override
    protected void onStop()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(batteryReceiver);
        super.onStop();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        sensorManager.registerListener(this, accelorometer, SensorManager.SENSOR_DELAY_UI);
        mGoogleApiClient.connect();
        shouldThreadRun = true;
        startRAMCPUThread();

        setDeviceMetrics();
        loadDeviceMetrics();
        loadBatteryMetrics();
        loadOtherMetrics();

        Log.i("MainActivity", "Setting Screen Name to 'Device Metrics'");

        sendDeviceandOtherInfoToGA();
    }

    private void sendDeviceandOtherInfoToGA()
    {
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


        //cpu info not included for now

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

        analytics.dispatchLocalHits();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        shouldThreadRun = false;

        //Disconnect from API onPause()
        if (mGoogleApiClient.isConnected())
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        sendBatteryInfoToGA();
    }

    private void sendBatteryInfoToGA()
    {
        String strDeviceSerialNumber = GetStoreMetrics.getSerialNumber();

        mTracker.setScreenName("Device Metrics");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /*
    private void getCurrentLocation()
    {
        String locationString="Device Latitude, Device Longitude:ERROR";
        String locationLat = "ERROR";
        String locationLng = "ERROR";
        DeviceMetric latMetric = getLatitude();
        DeviceMetric lngMetric = getLongitude();

        Location location;
        try
        {

            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(location!=null)
                {
                    //locationString = "Device latitude: " + String.valueOf(location.getLatitude()) + "\nDevice longitude: " + String.valueOf(location.getLongitude());
                    locationLat = String.valueOf(location.getLatitude());
                    locationLng = String.valueOf(location.getLongitude());
                }
                else
                {
                    locationLat = "ERROR";
                    locationLng = "ERROR";
                }
            }
            else if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES,this);
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if(location!=null)
                {
                    locationLat = String.valueOf(location.getLatitude());
                    locationLng = String.valueOf(location.getLongitude());
                }
                else
                {
                    locationLat = "ERROR";
                    locationLng = "ERROR";
                }

            }
            else
            {
                Toast.makeText(this, "Enable GPS or connect to a network to find location", Toast.LENGTH_SHORT).show();
                return;
            }


            latMetric.setDeviceMetricValue(locationLat);
            lngMetric.setDeviceMetricValue(locationLng);
            setLatitude(latMetric);
            setLongitude(lngMetric);
            otherListAdapter.notifyDataSetChanged();
        }
        catch (SecurityException ex)
        {
            ex.printStackTrace();
        }

    }
    */

    private int getCpuUsageStatistic()
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

    private String executeTop() {
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
            Toast.makeText(this,"Please enable Location services, if not enabled already",Toast.LENGTH_SHORT).show();
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
            //setLatitude(metricLat);

            metricLng.setDeviceMetricName("Longitude");
            metricLng.setDeviceMetricValue(String.valueOf(currentLongitude));
            if(currentLongitude<=0)
                metricLng.setIsAttentionRequired(1);
            else
                metricLng.setIsAttentionRequired(0);

            //setLongitude(metricLng);

        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        if(connectionResult.hasResolution())
        {
            try
            {
                connectionResult.startResolutionForResult(this,CONNECTION_FAILURE_RESOLUTION_REQUEST);
            }
            catch (IntentSender.SendIntentException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            Log.e("Error","Location Services failed with the code"+connectionResult.getErrorCode());

            if(connectionResult.getErrorCode() == ConnectionResult.SERVICE_MISSING)
            {
                Toast.makeText(this,"Location Services not available on this device", Toast.LENGTH_SHORT).show();
            }
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

            //setLatitude(latMetric);
            //setLongitude(lngMetric);

            otherListAdapter.notifyDataSetChanged();
        }
    }

}
