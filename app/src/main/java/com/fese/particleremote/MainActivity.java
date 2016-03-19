package com.fese.particleremote;

import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class MainActivity extends AppCompatActivity {


    private Toolbar toolbar;
    private String emailKey = "email";
    private String passwordKey = "password";
    private RecyclerView rv;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String[] ParticleFunctions = new String[4];

    private List<ParticleDevice> RVdevices;
    private List<io.particle.android.sdk.cloud.ParticleDevice> availableDevices;
    private io.particle.android.sdk.cloud.ParticleDevice selectedDevice;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: Improve loading time: Save device List in sharedPref. (set Status always to offline)

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.blue, R.color.green);
        rv = (RecyclerView) findViewById(R.id.deviceList);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);

        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setActionBar(toolbar);

        RVdevices = new ArrayList<>();
        RVAdapter adapter = new RVAdapter(RVdevices);
        rv.setAdapter(adapter);

        ParticleCloudSDK.init(this);


        RVAdapter.DeviceViewHolder.setOnParticleDeviceClickedListener(new RVAdapter.OnParticleDeviceClickedListener() {
            @Override
            public void onParticleDeviceClicked(String deviceID) {
                if (availableDevices != null) {
                    for (io.particle.android.sdk.cloud.ParticleDevice CloudDevice : availableDevices) {
                        if (CloudDevice.getID().equals(deviceID)) {
                            selectedDevice = CloudDevice;
                            startParticleFunctionDialog(deviceID);
                        } else if (deviceID.equals("test device")) {
                            Toaster.l(MainActivity.this, "Selected device is a virtual test device!");
                        } else {
                            Toaster.l(MainActivity.this, "Error");
                        }

                    }
                }
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadParticleDeviceList();
            }
        });


        //call Method on Create
        checkForSavedCredentials();
        initializeParticleFunctions();


    }

    private void initializeAdapter() {
        RVAdapter adapter = new RVAdapter(RVdevices);
        rv.setAdapter(adapter);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void initializeTestDeviceList() {
        RVdevices.add(new ParticleDevice("hamster_power", "test device", "ELECTRON", true));
        RVdevices.add(new ParticleDevice("bobcat_mutant", "test device", "ELECTRON", false));
        RVdevices.add(new ParticleDevice("normal_pizza", "test device", "CORE", true));
        RVdevices.add(new ParticleDevice("turtle_turkey", "test device", "PHOTON", true));

    }

    private void initializeParticleFunctions(){
        //TODO: Load available Functions from Particle Cloud
        //TODO: Save Strings in strings.xml
        ParticleFunctions[0] = "Toggle LED";
        ParticleFunctions[1] = "Switch Relays";
        ParticleFunctions[2] = "Read temperature & humidity";
        ParticleFunctions[3] = "more coming soon...";
    }

    //TODO: show hint if device is offline
    private void startParticleFunctionDialog(final String deviceID){
        new MaterialDialog.Builder(MainActivity.this)
                .title(R.string.title_function_dialog_list)
                .items(ParticleFunctions)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        switch (which) {
                            case 0:
                                Intent intentToggleLED = new Intent(MainActivity.this, tmp_led_toggle.class);
                                intentToggleLED.putExtra("deviceID", deviceID);
                                MainActivity.this.startActivity(intentToggleLED);
                                break;
                            case 1:
                                Intent intentRelay = new Intent(MainActivity.this, RelayActivity.class);
                                intentRelay.putExtra("deviceID", deviceID);
                                MainActivity.this.startActivity(intentRelay);
                                break;
                            case 2:
                                Intent intentTempHumi = new Intent(MainActivity.this, TempHumiActivity.class);
                                intentTempHumi.putExtra("deviceID", deviceID);
                                MainActivity.this.startActivity(intentTempHumi);
                                break;
                            case 3:
                                Toaster.l(MainActivity.this, "Coming soon...");
                                break;
                            case 4:
                                Toaster.l(MainActivity.this, "Coming soon...");
                                break;
                        }
                    }
                })
                .show();

        //Intent intentToggleLED = new Intent(MainActivity.this, tmp_led_toggle.class);
        //MainActivity.this.startActivity(intentToggleLED);
    }

    private void checkForSavedCredentials() {
        SharedPreferences mPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        //load stored email and password
        String email = mPrefs.getString(emailKey, null);
        String password = mPrefs.getString(passwordKey, null);
        //TODO: Decrypt Password

        //Check if saved credentials are available
        if ((email != null) && (password != null)) {
            AutoLogin(email, password);
        } else {
            startLoginActivity();
        }
    }

public void AutoLogin(final String email, final String password) {
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Void>() {

            public Void callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                particleCloud.logIn(email, password);
                return null;
            }

            public void onSuccess(Void aVoid) {
                Toaster.l(MainActivity.this, "Logged in");

                loadParticleDeviceList();
            }

            public void onFailure(ParticleCloudException e) {
                Log.e("SOME_TAG", e.getBestMessage());
                Toaster.l(MainActivity.this, "Wrong credentials or no internet connectivity, please try again");
                startLoginActivity();
            }

        });
    }

    public void logout() {

        //delete credentials in SharedPreferences
        SharedPreferences mPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.remove(emailKey);
        editor.remove(passwordKey);
        editor.apply();


        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Void>() {
            public Void callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                particleCloud.logOut();
                return null;
            }

            public void onSuccess(Void aVoid) {
                Toaster.l(MainActivity.this, "Logged out");

                // show LoginActivity...
                startLoginActivity();
            }

            public void onFailure(ParticleCloudException e) {
                Log.e("SOME_TAG", e.getBestMessage());
                Toaster.l(MainActivity.this, "Log out failed!");
            }

        });

    }

    public void loadParticleDeviceList() {
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, List<io.particle.android.sdk.cloud.ParticleDevice>>() {
            public List<io.particle.android.sdk.cloud.ParticleDevice> callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                return particleCloud.getDevices();
            }

            public void onSuccess(List<io.particle.android.sdk.cloud.ParticleDevice> devices) {
                availableDevices = devices;
                RVdevices.clear();
                for (io.particle.android.sdk.cloud.ParticleDevice device : availableDevices) {
                    RVdevices.add(new ParticleDevice(device.getName(), device.getID(), device.getDeviceType().toString(), device.isConnected()));
                }
                initializeTestDeviceList();
                initializeAdapter();

            }

            public void onFailure(ParticleCloudException e) {
                Log.e("SOME_TAG", e.getBestMessage());
                Toaster.l(MainActivity.this, "No device found");
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
            case R.id.logout:
                logout();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }


}
