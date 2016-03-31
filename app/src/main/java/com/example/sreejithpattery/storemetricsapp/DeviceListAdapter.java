package com.example.sreejithpattery.storemetricsapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by 310722 on 3/30/2016.
 */
public class DeviceListAdapter extends ArrayAdapter<DeviceMetric>
{
    Context context;
    List<DeviceMetric> deviceMetrics;

    public DeviceListAdapter(Context context, int resource, List<DeviceMetric> objects)
    {
        super(context, resource, objects);
        this.context = context;
        this.deviceMetrics = objects;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View itemview = null;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (itemview == null) {
            itemview = inflater.inflate(R.layout.item_view, null);
            //get the current item
            DeviceMetric currentDeviceMetric = deviceMetrics.get(position);

            //fill the view
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
