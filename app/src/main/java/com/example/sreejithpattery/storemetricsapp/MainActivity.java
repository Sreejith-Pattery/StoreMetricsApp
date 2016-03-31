package com.example.sreejithpattery.storemetricsapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.Display;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class MainActivity extends Activity implements SensorEventListener,
        LocationListener
{
    private List<DeviceMetric> deviceMetrics = new ArrayList<>();
    private List<DeviceMetric> batteryMetrics = new ArrayList<>();
    private List<DeviceMetric> otherMetrics = new ArrayList<>();

    DeviceMetric cpuInfo;
    DeviceMetric ramInfo;
    DeviceMetric ramPerc;
    DeviceMetric isMoving;
    DeviceMetric latitude;
    DeviceMetric longitude;

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        cpuInfo = new DeviceMetric("Current CPU Usage %","");
        ramInfo = new DeviceMetric("RAM Usage MB","");
        ramPerc = new DeviceMetric("RAM Usage %","");
        isMoving = new DeviceMetric("Device Movement","");
        latitude = new DeviceMetric("Device latitude","");
        longitude = new DeviceMetric("Device longitude","");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelorometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        this.registerReceiver(this.batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        getCurrentLocation();


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

    }

    private void loadOtherMetrics()
    {
        ArrayAdapter<DeviceMetric> otherListAdapter = new OtherMetricsAdapter(this,R.layout.item_view,otherMetrics);
        ListView lstOtherMetrics = (ListView) findViewById(R.id.lstOtherMetrics);
        lstOtherMetrics.setAdapter(otherListAdapter);
    }

    private void getOtherMetrics()
    {
        otherMetrics.clear();
        otherMetrics.add(getCpuInfo());
        otherMetrics.add(getRamInfo());
        otherMetrics.add(getRamPerc());
        otherMetrics.add(getIsMoving());
        otherMetrics.add(getLatitude());
        otherMetrics.add(getLongitude());
    }

    private void loadDeviceMetrics()
    {
        ArrayAdapter<DeviceMetric> deviceListAdapter = new DeviceListAdapter(this,R.layout.item_view,deviceMetrics);
        ListView lstDeviceMetrics = (ListView) findViewById(R.id.lstDeviceMetrics);
        lstDeviceMetrics.setAdapter(deviceListAdapter);
    }

    private void setDeviceMetrics()
    {
        deviceMetrics.clear();
        deviceMetrics.add(new DeviceMetric("Device Storage Remaining %",GetStoreMetrics.getAvailableInternalMemorySizePerc()));
        deviceMetrics.add(new DeviceMetric("Device Storage Remaining MB",GetStoreMetrics.getAvailableInternalMemorySize() + " out of " + GetStoreMetrics.getTotalInternalMemorySize()));
        deviceMetrics.add(new DeviceMetric("Foreground Application Bundle ID",getForegroundTask()));
        deviceMetrics.add(new DeviceMetric("Foreground Application Name",getCurrentApplicationName(getForegroundTask())));
        deviceMetrics.add(new DeviceMetric("Wifi Network", getCurrentSsid()));
        deviceMetrics.add(new DeviceMetric("Wifi Signal Strength",getCurrentSignalStrength() + " out of 5"));
        deviceMetrics.add(new DeviceMetric("Device IP",getIpAddress()));
        deviceMetrics.add(new DeviceMetric("Device Gateway",getDeviceGateway()));
        deviceMetrics.add(new DeviceMetric("Device Serial Number",GetStoreMetrics.getSerialNumber()));
        deviceMetrics.add(new DeviceMetric("Screen State", getScreenState()));
        deviceMetrics.add(new DeviceMetric("Screen Brightness",getScreenBrighness()));
        deviceMetrics.add(new DeviceMetric("Foreground Application version", "Version No " + getForegroundApplicationVersion(getForegroundTask())));
    }

    public String getScreenBrighness()
    {
        String screenBrightness = "ERROR";
        float curBrightnessValue=0;

        curBrightnessValue =  getWindow().getAttributes().screenBrightness;

        if(curBrightnessValue<0)
        {

            try {
                curBrightnessValue = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                screenBrightness = String.valueOf(curBrightnessValue)+" out of 255";
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        }
        else
        {
            screenBrightness = String.valueOf(curBrightnessValue)+" out of 1";
        }

        return screenBrightness;
    }

    public String getForegroundTask()
    {
        String currentApp = "NULL";

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR,-1);

            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,cal.getTimeInMillis(),System.currentTimeMillis());
            if(appList!=null && appList.size()>0)
            {
                SortedMap<Long,UsageStats> sortedMap = new TreeMap<>();
                for (UsageStats usagestat:appList
                        ) {
                    sortedMap.put(usagestat.getLastTimeUsed(),usagestat);
                }

                if(sortedMap!=null && !sortedMap.isEmpty())
                {
                    currentApp = sortedMap.get(sortedMap.lastKey()).getPackageName();
                }
            }
        }
        else
        {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> listProcesses= am.getRunningAppProcesses();
            if(!listProcesses.isEmpty())
            {
                currentApp = listProcesses.get(0).processName;
            }
        }

        return currentApp;
    }

    private WifiInfo getWifiInfo() {
        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        return wifiManager.getConnectionInfo();
    }

    private NetworkInfo getNetworkInfo() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

    public int getCurrentSignalStrength()
    {
        int level = -1;
        NetworkInfo networkInfo = getNetworkInfo();
        if(networkInfo.isConnected())
        {
            final WifiInfo wifiInfo = getWifiInfo();
            level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
        }

        return level;
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
            final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            dhcp_ipaddress = Formatter.formatIpAddress(dhcpInfo.serverAddress);
        }

        return dhcp_ipaddress;
    }

    @SuppressWarnings("deprecation")
    public String getDeviceGateway()
    {
        String dhcp_gateway = "ERROR";
        NetworkInfo networkInfo = getNetworkInfo();
        if(networkInfo.isConnected())
        {
            final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
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
        PackageManager packageManager = getPackageManager();
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

    public String getScreenState()
    {
        String screenState = "ERROR";
        if(Build.VERSION.SDK_INT >= 20)
        {
            DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
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
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
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
        PackageManager packageManager = getApplicationContext().getPackageManager();
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

    private void startRAMCPUThread()
    {
        handler = new Handler();
        r = new Runnable() {
            @Override
            public void run()
            {
                if(shouldThreadRun) {
                    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                    ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

                    activityManager.getMemoryInfo(memoryInfo);
                    avlMem = memoryInfo.availMem / 1048576;
                    totMem = memoryInfo.totalMem / 1048576;
                    usedMem = ((memoryInfo.totalMem - memoryInfo.availMem)) / 1048576;

                    DeviceMetric cpuInfo = getCpuInfo();
                    DeviceMetric ramInfo = getRamInfo();
                    DeviceMetric ramPerc = getRamPerc();

                    cpuInfo.setDeviceMetricValue(String.valueOf(readUsage() * 100) + " %");
                    ramInfo.setDeviceMetricValue(usedMem + " MB out of " + totMem + " MB");
//                textViewCpuInfo.setText("Current CPU Usage %:\n" + String.valueOf(readUsage() * 100) + " %");
//                textViewRAMInfo.setText("RAM Usage MB :\n" + usedMem + " MB out of " + totMem + " MB\n");


                    DecimalFormat df = new DecimalFormat("##.##");

                    if (totMem > 0) {
                        ramPerc.setDeviceMetricValue(df.format(((float) (totMem - avlMem) / (float) totMem) * 100) + " %");
                        //.append("RAM Usage % :"+df.format(((float)(totMem-avlMem)/(float)totMem)*100)+" %");
                    }

                    setCpuInfo(cpuInfo);
                    setRamInfo(ramInfo);
                    setRamPerc(ramPerc);

                    getOtherMetrics();
                    loadOtherMetrics();

                    handler.postDelayed(this, 1000);
                }
            }

        };

        handler.postDelayed(r, 1000);

    }

    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver()
    {


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
            float fltemperature = temperature/10;
            int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);




            String strHealth = "ERROR";
            switch (health)
            {
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

            String strBatteryPlugged="ERROR";
            switch (plugged)
            {
                case BatteryManager.BATTERY_PLUGGED_AC:
                    strBatteryPlugged="BATTERY PLUGGED AC";
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB:
                    strBatteryPlugged="BATTERY PLUGGED USB";
                    break;
                case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                    strBatteryPlugged="BATTERY PLUGGED WIRELESS";
                    break;
                case 0:
                    strBatteryPlugged="BATTERY DISCHARGING";
                    break;
                default:
                    strBatteryPlugged="ERROR";
                    break;
            }

            if(batteryMetrics.size()>0)
                batteryMetrics.clear();

            batteryMetrics.add(new DeviceMetric("Battery Health",strHealth));
            batteryMetrics.add(new DeviceMetric("Battery Charge Level",level + " out of " +scale));
            batteryMetrics.add(new DeviceMetric("Battery Temperature",fltemperature + " Degrees Celsius"));
            batteryMetrics.add(new DeviceMetric("Device charging status",strBatteryPlugged));


            if(Build.MODEL.equals("MC40N0"))
            {
                String serialnumber = "ERROR";
                String mfd = "ERROR";
                int cycle=0;

                mfd = intent.getExtras().getString("mfd");
                serialnumber = intent.getExtras().getString("serialnumber");
                cycle = intent.getExtras().getInt("cycle");

                batteryMetrics.add(new DeviceMetric("Battery Manufacture Date",mfd));
                batteryMetrics.add(new DeviceMetric("Battery Serial Number",serialnumber));

            }


            ArrayAdapter<DeviceMetric> deviceListAdapter = new DeviceListAdapter(getBaseContext(),R.layout.item_view,batteryMetrics);
            ListView lstDeviceMetrics = (ListView) findViewById(R.id.lstBatteryMetrics);
            lstDeviceMetrics.setAdapter(deviceListAdapter);

        }
    };

    public DeviceMetric getCpuInfo() {
        return cpuInfo;
    }

    public void setCpuInfo(DeviceMetric cpuInfo) {
        this.cpuInfo = cpuInfo;
    }

    public DeviceMetric getRamInfo() {
        return ramInfo;
    }

    public void setRamInfo(DeviceMetric ramInfo) {
        this.ramInfo = ramInfo;
    }

    public DeviceMetric getRamPerc() {
        return ramPerc;
    }

    public void setRamPerc(DeviceMetric ramPerc) {
        this.ramPerc = ramPerc;
    }

    public DeviceMetric getIsMoving() {
        return isMoving;
    }

    public void setIsMoving(DeviceMetric isMoving) {
        this.isMoving = isMoving;
    }

    public DeviceMetric getLatitude() {
        return latitude;
    }

    public void setLatitude(DeviceMetric latitude) {
        this.latitude = latitude;
    }

    public DeviceMetric getLongitude() {
        return longitude;
    }

    public void setLongitude(DeviceMetric longitude) {
        this.longitude = longitude;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if(location!=null)
        {
            DeviceMetric latMetric = getLatitude();
            DeviceMetric lngMetric = getLongitude();

            latMetric.setDeviceMetricValue(String.valueOf(location.getLatitude()));
            lngMetric.setDeviceMetricValue(String.valueOf(location.getLongitude()));

            setLatitude(latMetric);
            setLongitude(lngMetric);

            getOtherMetrics();
            loadOtherMetrics();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {

        DeviceMetric isMovingMetric = getIsMoving();

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
                //isMoving = "MOVING";
            }
            else
            {
                //isMoving = "NOT MOVING";
                isMovingMetric.setDeviceMetricValue("NOT MOVING");
            }

            setIsMoving(isMovingMetric);
            //txtIsMoving.setText("Device Movement:\n" + isMoving);

            getOtherMetrics();
            loadOtherMetrics();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        sensorManager.registerListener(this, accelorometer, SensorManager.SENSOR_DELAY_UI);
        shouldThreadRun = true;
        startRAMCPUThread();

        setDeviceMetrics();
        loadDeviceMetrics();
        getOtherMetrics();
        loadOtherMetrics();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        shouldThreadRun = false;
    }

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
            }
        }
        catch (SecurityException ex)
        {
            ex.printStackTrace();
        }

        latMetric.setDeviceMetricValue(locationLat);
        lngMetric.setDeviceMetricValue(locationLng);
        setLatitude(latMetric);
        setLongitude(lngMetric);
    }
}
