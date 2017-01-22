package com.fese.particleremote;

import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.IOException;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;

/**
 * Created by Fabia on 21.01.2016.
 */

class MyParticleDevice {
    String deviceName;
    String deviceID;
    String model;
    boolean isConnected;

    MyParticleDevice(String deviceName, String deviceID, String model, boolean isConnected) {
        this.deviceName = deviceName;
        this.deviceID = deviceID;
        this.model = model;
        this.isConnected = isConnected;
    }

}

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.DeviceViewHolder> {


    public interface OnParticleDeviceClickedListener {
        void onParticleDeviceClicked(String deviceID);
    }


    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView deviceName;
        TextView deviceID;
        TextView model;
        TextView status;
        ImageView devicePhoto;
        ImageView statusLED;
        View view;



        private static OnParticleDeviceClickedListener mParticleClickListener;
        public static void setOnParticleDeviceClickedListener(OnParticleDeviceClickedListener l) {
            mParticleClickListener = l;
        }



        DeviceViewHolder(View itemView) {
            super(itemView);
            cv = (CardView) itemView.findViewById(R.id.cv_device);
            deviceName = (TextView) itemView.findViewById(R.id.deviceName);
            deviceID = (TextView) itemView.findViewById(R.id.deviceID);
            model = (TextView) itemView.findViewById(R.id.model);
            status = (TextView) itemView.findViewById(R.id.deviceStatus);
            devicePhoto = (ImageView) itemView.findViewById(R.id.devicePhoto);
            statusLED = (ImageView) itemView.findViewById(R.id.statusLED);
            view = itemView;


            view.setOnClickListener(new View.OnClickListener(){
                @Override public void onClick(View itemView){

                    if (mParticleClickListener != null) {
                        final String ID = deviceID.getText().toString();
                        mParticleClickListener.onParticleDeviceClicked(ID);

                    }

                }

            });
        }
    }

    static List<MyParticleDevice> devices;

    RVAdapter(List<MyParticleDevice> devices){
        RVAdapter.devices = devices;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview, viewGroup, false);
        return new DeviceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RVAdapter.DeviceViewHolder deviceViewHolder, int i) {
        deviceViewHolder.deviceName.setText(devices.get(i).deviceName);
        deviceViewHolder.deviceID.setText(devices.get(i).deviceID);

        if (devices.get(i).isConnected){
            deviceViewHolder.statusLED.setImageResource(R.drawable.online_dot);
            deviceViewHolder.status.setText("Online");
        }
        else{
            deviceViewHolder.statusLED.setImageResource(R.drawable.offline_dot);
            deviceViewHolder.status.setText("Offline");
        }

        switch (devices.get(i).model) {
            case "PHOTON":
                deviceViewHolder.model.setText("Photon");
                deviceViewHolder.devicePhoto.setImageResource(R.drawable.photon_vector);
                break;
            case "ELECTRON":
                deviceViewHolder.model.setText("Electron");
                deviceViewHolder.devicePhoto.setImageResource(R.drawable.electron_vector);
                break;
            case "CORE":
                deviceViewHolder.model.setText("Core");
                deviceViewHolder.devicePhoto.setImageResource(R.drawable.core_vector);
                break;
            default:
                deviceViewHolder.model.setText(devices.get(i).model);
        }

    }



    @Override
    public int getItemCount() {
        return devices.size();
    }
}

