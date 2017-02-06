package com.fese.particleremote;

import android.content.Intent;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.utils.Async;

import static com.fese.particleremote.R.color.colorAccent;


public class MainActivity extends AppCompatActivity {

    private RecyclerView rv;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String[] ParticleFunctions = new String[4];
    private List<MyParticleDevice> RVdevices;
    private List<io.particle.android.sdk.cloud.ParticleDevice> availableDevices;
    private SharedPreferences deviceListSharedPref;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.blue, R.color.green);
        }
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
                    for (MyParticleDevice device : RVdevices) {
                        if (deviceID.equals("test device")) {
                            Snackbar snackbarInfo = Snackbar
                                    .make(rv, "Selected device is a virtual test device!", Snackbar.LENGTH_LONG);
                            snackbarInfo.show();
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

        //Setup a new Particle Device
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_setupDevice);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ParticleDeviceSetupLibrary.startDeviceSetup(MainActivity.this);
                }
            });
        }


        //call Method on Create
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        ParticleCloudSDK.init(this);
        ParticleDeviceSetupLibrary.init(this.getApplicationContext(), MainActivity.class);
        checkLoginStatus();
        initializeParticleDeviceFunctions();



    }


    private void initializeTestDeviceList() {
        RVdevices.add(new MyParticleDevice("*Electron Test Device", "test device", "ELECTRON", false));
        RVdevices.add(new MyParticleDevice("*Core Test Device", "test device", "CORE", true));
        RVdevices.add(new MyParticleDevice("*Electron Test Device", "test device", "ELECTRON", false));
        RVdevices.add(new MyParticleDevice("*Electron Test Device", "test device", "ELECTRON", false));
        RVdevices.add(new MyParticleDevice("*Electron Test Device", "test device", "ELECTRON", false));
        RVdevices.add(new MyParticleDevice("*Electron Test Device", "test device", "ELECTRON", false));


    }

    private void initializeParticleDeviceFunctions(){


        ParticleFunctions[0] = getString(R.string.particle_function_dialog_item1);
        ParticleFunctions[1] = getString(R.string.particle_function_dialog_item2);
        ParticleFunctions[2] = getString(R.string.particle_function_dialog_item3);
        ParticleFunctions[3] = getString(R.string.particle_function_dialog_item4);
    }

    private void startParticleFunctionDialog(final String deviceID){

        final MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(new MaterialSimpleListAdapter.Callback() {
            @Override
            public void onMaterialListItemSelected(MaterialDialog dialog, int index, MaterialSimpleListItem item) {
                switch (index) {
                    case 0:
                        Intent intentRelay = new Intent(MainActivity.this, RelayScrollingActivity.class);
                        intentRelay.putExtra("deviceID", deviceID);
                        MainActivity.this.startActivity(intentRelay);
                        dialog.dismiss();
                        break;
                    case 1:
                        Intent intentTempHumi = new Intent(MainActivity.this, TempHumiActivity.class);
                        intentTempHumi.putExtra("deviceID", deviceID);
                        MainActivity.this.startActivity(intentTempHumi);
                        dialog.dismiss();
                        break;
                    case 2:
                        Intent intentTempHoneywell = new Intent(MainActivity.this, TempHoneywellActivity.class);
                        intentTempHoneywell.putExtra("deviceID", deviceID);
                        MainActivity.this.startActivity(intentTempHoneywell);
                        dialog.dismiss();
                        break;
                    case 3:
                        Snackbar snackbarInfo = Snackbar
                                .make(rv, "Comming soon...", Snackbar.LENGTH_LONG);
                        snackbarInfo.show();
                        dialog.dismiss();
                        break;

                }
            }
        });

        adapter.add(new MaterialSimpleListItem.Builder(this)
                .content(R.string.particle_function_dialog_item1)
                .icon(R.drawable.ic_led_on_grey600_48dp)
                .backgroundColor(Color.WHITE)
                //.iconPaddingDp(2)
                .build());
        adapter.add(new MaterialSimpleListItem.Builder(this)
                .content(R.string.particle_function_dialog_item2)
                .icon(R.drawable.ic_chart_line_grey600_48dp)
                .backgroundColor(Color.WHITE)
                .build());
        adapter.add(new MaterialSimpleListItem.Builder(this)
                .content(R.string.particle_function_dialog_item3)
                .icon(R.drawable.ic_thermometer_lines_grey600_48dp)
                .backgroundColor(Color.WHITE)
                .build());

        adapter.add(new MaterialSimpleListItem.Builder(this)
                .content(R.string.particle_function_dialog_item4)
                .icon(R.drawable.ic_access_point_grey600_48dp)
                .backgroundColor(Color.WHITE)
                .build());

        new MaterialDialog.Builder(this)
                .title(R.string.title_function_dialog_list)
                .adapter(adapter, null)
                .show();


        /*
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
                                Snackbar snackbarInfo = Snackbar
                                        .make(rv, "Comming soon...", Snackbar.LENGTH_LONG);
                                snackbarInfo.show();
                                break;

                        }
                    }
                })
                .show();


                */


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
        prefsEditor.putString(getString(R.string.saved_particle_device_shared_pref_key), json);
        prefsEditor.apply();

    }

    private void loadDeviceList(){
        Gson gson = new Gson();
        //load the Json String. If no string is available the device list is null
        String json = deviceListSharedPref.getString(getString(R.string.saved_particle_device_shared_pref_key), "");
        //transform the Json string into the original device list
        RVdevices = gson.fromJson(json, new TypeToken<List<MyParticleDevice>>() {}.getType());

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
        prefsEditor.remove(getString(R.string.saved_particle_device_shared_pref_key));
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

            public void onSuccess( List<io.particle.android.sdk.cloud.ParticleDevice> devices) {
                availableDevices = devices;
                RVdevices.clear();
                for (io.particle.android.sdk.cloud.ParticleDevice device : availableDevices) {
                    RVdevices.add(new MyParticleDevice(device.getName(), device.getID(), device.getDeviceType().toString(), device.isConnected()));
                }
                initializeTestDeviceList();
                rv.getAdapter().notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
                storeDeviceList();
            }

            public void onFailure(ParticleCloudException e) {
                Log.e("SOME_TAG", e.getBestMessage());

                Snackbar snackbarError = Snackbar
                        .make(rv, e.getBestMessage(), Snackbar.LENGTH_LONG);
                snackbarError.show();
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
                Intent intentSettings = new Intent(MainActivity.this, UserSettingsActivity.class);
                MainActivity.this.startActivity(intentSettings);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }


}
