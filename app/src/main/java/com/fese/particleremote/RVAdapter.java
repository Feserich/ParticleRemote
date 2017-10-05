package com.fese.particleremote;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;


import java.util.List;

import static com.fese.particleremote.RVAdapter.DeviceViewHolder.mDeviceMenuClickListener;
import static com.fese.particleremote.RVAdapter.DeviceViewHolder.mParticleClickListener;


/**
 * Created by Fabia on 21.01.2016.
 */

class MyParticleDevice {
    String deviceName;
    String deviceID;
    String model;
    boolean isConnected;
    boolean hideDevice;
    Integer[] availableFunctions;


    MyParticleDevice(String deviceName, String deviceID, String model, boolean isConnected, boolean hideDevice, Integer[] availableFunctions) {
        this.deviceName = deviceName;
        this.deviceID = deviceID;
        this.model = model;
        this.isConnected = isConnected;
        this.hideDevice = hideDevice;
        this.availableFunctions = availableFunctions;
    }

}

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.DeviceViewHolder> {



    public interface OnParticleDeviceClickedListener {
        void onParticleDeviceClicked(String deviceID);
    }

    public interface OnDeviceMenuItemClickListener {
        void onDeviceMenuItemClicked(String deviceID, MenuItem menuItem);
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
        ImageButton ib_device_menu;



        static OnParticleDeviceClickedListener mParticleClickListener;
        public static void setOnParticleDeviceClickedListener(OnParticleDeviceClickedListener l) {
            mParticleClickListener = l;
        }


        static OnDeviceMenuItemClickListener mDeviceMenuClickListener;
        public static void setOnMenuItemClickListener(OnDeviceMenuItemClickListener l) {
            mDeviceMenuClickListener = l;
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

            ib_device_menu = (ImageButton) view.findViewById(R.id.imageButton_device_menu);




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
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_device, viewGroup, false);
        return new DeviceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RVAdapter.DeviceViewHolder deviceViewHolder, final int i) {
        deviceViewHolder.deviceName.setText(devices.get(i).deviceName);
        deviceViewHolder.deviceID.setText("ID: " + devices.get(i).deviceID);

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


        deviceViewHolder.ib_device_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupMenu(deviceViewHolder.ib_device_menu, devices.get(i).deviceID);
            }
        });

        deviceViewHolder.view.setOnClickListener(new View.OnClickListener(){
            @Override public void onClick(View itemView){

                if (mParticleClickListener != null) {
                    mParticleClickListener.onParticleDeviceClicked(devices.get(i).deviceID);

                }
            }
        });

    }

    private void showPopupMenu(View view, final String deviceID) {
        // inflate menu
        final PopupMenu popup = new PopupMenu(view.getContext(),view );
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.device_popup_menu, popup.getMenu());
        //popup.setOnMenuItemClickListener(mDeviceMenuClickListener);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                mDeviceMenuClickListener.onDeviceMenuItemClicked(deviceID, menuItem);
                return false;
            }
        });


        //mDeviceMenuClickListener.onDeviceMenuItemClicked(deviceID, popup);
        popup.show();
    }





    @Override
    public int getItemCount() {
        return devices.size();
    }
}


