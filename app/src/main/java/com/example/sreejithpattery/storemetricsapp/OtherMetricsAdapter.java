package com.example.sreejithpattery.storemetricsapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by 310722 on 3/30/2016.
 */
public class OtherMetricsAdapter extends ArrayAdapter<DeviceMetric>
{
    Context context;
    List<DeviceMetric> deviceMetrics;
    MainActivity mainActivity;

    public OtherMetricsAdapter(Context context, int resource, List<DeviceMetric> objects)
    {
        super(context, resource, objects);
        this.context = context;
        this.deviceMetrics = objects;
        mainActivity = (MainActivity) context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View itemview = null;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (itemview == null)
        {
            itemview = inflater.inflate(R.layout.item_view, null);
            //get the current item
            DeviceMetric currentDeviceMetric = deviceMetrics.get(position);

            //fill the view
            switch (currentDeviceMetric.getDeviceMetricName())
            {
                case "Current CPU Usage %":
                    currentDeviceMetric = StoreMetricsContainer.getCpuInfo();
                    break;

                case "RAM Usage MB":
                    currentDeviceMetric = StoreMetricsContainer.getRamInfo();
                    break;

                case "RAM Usage %":
                    currentDeviceMetric = StoreMetricsContainer.getRamPerc();
                    break;

                case "Device Movement":
                    currentDeviceMetric = StoreMetricsContainer.getIsMoving();
                    break;

                case "Device latitude":
                    currentDeviceMetric = StoreMetricsContainer.getLatitude();
                    break;

                case "Device longitude":
                    currentDeviceMetric = StoreMetricsContainer.getLongitude();
                    break;

                default:
                    currentDeviceMetric = deviceMetrics.get(position);
                    break;
            }

            TextView txtUpc = (TextView) itemview.findViewById(R.id.txtMetricLabel);
            txtUpc.setText((CharSequence) String.valueOf(currentDeviceMetric.getDeviceMetricName()));


            TextView txtDesc = (TextView) itemview.findViewById(R.id.txtMetricValue);
            txtDesc.setText((CharSequence) currentDeviceMetric.getDeviceMetricValue());
            return itemview;
        }
        else
        {
            itemview = convertView;
        }

        return convertView;
    }
}
