package com.fese.particleremote;

import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;


import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class MainActivity extends AppCompatActivity {



    private String emailKey = "email";  private String passwordKey = "password";

    private RecyclerView rv;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String[] ParticleFunctions = new String[4];
    private List<ParticleDevice> RVdevices;
    private List<io.particle.android.sdk.cloud.ParticleDevice> availableDevices;
    private SharedPreferences deviceListSharedPref;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.blue, R.color.green);
        rv = (RecyclerView) findViewById(R.id.deviceList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(linearLayoutManager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        deviceListSharedPref = getPreferences(MODE_PRIVATE);

        RVdevices = new ArrayList<>();
        loadDeviceList();                       //must be called before adapter gets initialized

        RVAdapter adapter = new RVAdapter(RVdevices);
        rv.setAdapter(adapter);


        RVAdapter.DeviceViewHolder.setOnParticleDeviceClickedListener(new RVAdapter.OnParticleDeviceClickedListener() {
            @Override
            public void onParticleDeviceClicked(String deviceID) {
                if (RVdevices != null) {
                    for (ParticleDevice device : RVdevices) {
                        if (deviceID.equals("test device")) {
                            Toaster.l(MainActivity.this, "Selected device is a virtual test device!");
                        }
                        else if (device.deviceID.equals(deviceID)) {
                            startParticleFunctionDialog(deviceID);
                        }

                    }
                }
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getParticleDeviceListFromCloud();
            }
        });



        //show SwipeRefreshLayout onCreate till getParticleDeviceListFromCloud has been completed
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });


        //call Method on Create
        ParticleCloudSDK.init(this);
        checkLoginStatus();
        initializeParticleDeviceFunctions();



    }


    private void initializeTestDeviceList() {
        RVdevices.add(new ParticleDevice("*Electron Test Device", "test device", "ELECTRON", false));
        RVdevices.add(new ParticleDevice("*Core Test Device", "test device", "CORE", true));


    }

    private void initializeParticleDeviceFunctions(){

        ParticleFunctions[0] = "Switch Relays";
        ParticleFunctions[1] = "Read temperature & humidity";
        ParticleFunctions[2] = "Set Honeywell temperature";
        ParticleFunctions[3] = "Send commands over 433Mhz radio";
    }

    private void startParticleFunctionDialog(final String deviceID){

        new MaterialDialog.Builder(MainActivity.this)
                .title(R.string.title_function_dialog_list)
                .items(ParticleFunctions)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        switch (which) {
                            case 0:
                                Intent intentRelay = new Intent(MainActivity.this, RelayScrollingActivity.class);
                                intentRelay.putExtra("deviceID", deviceID);
                                MainActivity.this.startActivity(intentRelay);
                                break;
                            case 1:
                                Intent intentTempHumi = new Intent(MainActivity.this, TempHumiActivity.class);
                                intentTempHumi.putExtra("deviceID", deviceID);
                                MainActivity.this.startActivity(intentTempHumi);
                                break;
                            case 2:
                                Intent intentTempHoneywell = new Intent(MainActivity.this, TempHoneywellActivity.class);
                                intentTempHoneywell.putExtra("deviceID", deviceID);
                                MainActivity.this.startActivity(intentTempHoneywell);
                                break;
                            case 3:
                                Toaster.l(MainActivity.this, "Coming soon...");
                                break;

                        }
                    }
                })
                .show();


    }

    private void checkLoginStatus() {
        if (ParticleCloudSDK.getCloud().isLoggedIn()){
            getParticleDeviceListFromCloud();
        }
        else startLoginActivity();

    }

    private void storeDeviceList(){
        SharedPreferences.Editor prefsEditor = deviceListSharedPref.edit();
        //transform the relay list to a string
        Gson gson = new Gson();
        String json = gson.toJson(RVdevices);
        //store this Json string in Shared Preferences
        prefsEditor.putString("Saved Particle Devices:", json);
        prefsEditor.commit();

    }

    private void loadDeviceList(){
        Gson gson = new Gson();
        //load the Json String. If no string is available the device list is null
        String json = deviceListSharedPref.getString("Saved Particle Devices:", "");
        //transform the Json string into the original device list
        RVdevices = gson.fromJson(json, new TypeToken<List<ParticleDevice>>() {}.getType());

        //if no String is available RVdevices is set to null!
        if (RVdevices == null){
            //create a new empty ArrayList
            RVdevices = new ArrayList<>();
        }

        //rv.getAdapter().notifyDataSetChanged();

    }

    public void logout() {

        /*
        //delete credentials in SharedPreferences
        SharedPreferences mPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.remove(emailKey);
        editor.remove(passwordKey);
        editor.apply();
        */

        //delete device List
        SharedPreferences.Editor prefsEditor = deviceListSharedPref.edit();
        prefsEditor.remove("Saved Particle Devices:");
        prefsEditor.apply();

        //Logout the user. Clear session and access token
        ParticleCloudSDK.getCloud().logOut();

        startLoginActivity();


    }

    public void getParticleDeviceListFromCloud() {


        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, List<io.particle.android.sdk.cloud.ParticleDevice>>() {
            public List<io.particle.android.sdk.cloud.ParticleDevice> callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                return ParticleCloudSDK.getCloud().getDevices();
            }

            public void onSuccess(List<io.particle.android.sdk.cloud.ParticleDevice> devices) {
                availableDevices = devices;
                RVdevices.clear();
                for (io.particle.android.sdk.cloud.ParticleDevice device : availableDevices) {
                    RVdevices.add(new ParticleDevice(device.getName(), device.getID(), device.getDeviceType().toString(), device.isConnected()));
                }
                initializeTestDeviceList();
                rv.getAdapter().notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
                storeDeviceList();
            }

            public void onFailure(ParticleCloudException e) {
                Log.e("SOME_TAG", e.getBestMessage());
                Toaster.l(MainActivity.this, e.getBestMessage().toString());
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });



    }

    public void startLoginActivity() {
        Intent intentLogin = new Intent(MainActivity.this, LoginActivity.class);
        MainActivity.this.startActivity(intentLogin);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.menu_logout:
                logout();
                return true;

            case R.id.menu_refresh:
                getParticleDeviceListFromCloud();
                //show SwipeRefreshLayout onCreate till getParticleDeviceListFromCloud has been completed
                mSwipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(true);
                    }
                });
                return true;

            case R.id.menu_settings:
                return true;



            default:
                return super.onOptionsItemSelected(item);
        }


    }


}
