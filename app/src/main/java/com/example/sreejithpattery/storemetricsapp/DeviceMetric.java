package com.example.sreejithpattery.storemetricsapp;

/**
 * Created by 310722 on 3/30/2016.
 */
public class DeviceMetric
{
    public DeviceMetric() {
    }

    String deviceMetricName;
    String deviceMetricValue;

    public String getDeviceMetricName() {
        return deviceMetricName;
    }

    public DeviceMetric(String deviceMetricName, String deviceMetricValue) {
        this.deviceMetricName = deviceMetricName;
        this.deviceMetricValue = deviceMetricValue;
    }

    public void setDeviceMetricName(String deviceMetricName) {
        this.deviceMetricName = deviceMetricName;
    }

    public String getDeviceMetricValue() {
        return deviceMetricValue;
    }

    public void setDeviceMetricValue(String deviceMetricValue) {
        this.deviceMetricValue = deviceMetricValue;
    }
}
