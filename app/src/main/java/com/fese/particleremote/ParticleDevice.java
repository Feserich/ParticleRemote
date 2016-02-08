package com.fese.particleremote;

/**
 * Created by Fabia on 21.01.2016.
 */
public class ParticleDevice {
    String deviceName;
    String deviceID;
    String model;
    boolean isConnected;

    ParticleDevice(String deviceName, String deviceID, String model, boolean isConnected) {
        this.deviceName = deviceName;
        this.deviceID = deviceID;
        this.model = model;
        this.isConnected = isConnected;
    }


}
