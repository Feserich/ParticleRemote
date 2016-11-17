package com.fese.particleremote;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.primitives.Booleans;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.cloud.*;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class RelayScrollingActivity extends AppCompatActivity {

    CollapsingToolbarLayout collapsingToolbar;
    private String myParticleID = null;
    private RecyclerView recyclerView;
    RVadapterRelay relayAdapter;
    private List<Relay> listRelays;
    private Relay tempDeletedRelay;
    private String editPin;
    private String editRelayName;
    private boolean editSwitchConfirmation;
    private Spinner pinSpinner;
    private SharedPreferences relaySharedPref;
    private ParticleDevice myParticleDevice;
    private List<String> functionCommandList;
    private String commandValue;
    private boolean relayIsSwitching;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relay_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        collapsingToolbar.setTitle("Toggle Relay");

        relaySharedPref = getPreferences(MODE_PRIVATE);

        recyclerView = (RecyclerView) findViewById(R.id.relayList);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        //get the ID of the selected Particle Device from the MainActivity
        myParticleID = (String) getIntent().getSerializableExtra("deviceID");



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddNewRelayPopup();
            }
        });

        RVadapterRelay.RelayViewHolder.setOnItemClickListener(new RVadapterRelay.OnItemClickListener() {
            @Override
            public void onItemClicked(String pin) {
                for (Relay relay: listRelays){
                    if (relay.pin.equals(pin)){
                        if (relay.switchConfirmation) {showConfirmationPopup(relay);}
                        else {toggleRelay(relay);}
                    }
                }


            }
        });

        RVadapterRelay.RelayViewHolder.setOnItemLongClickListener(new RVadapterRelay.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(String pin) {
                //TODO: show relay properties (edit name, edit pin (dropdown menu), checkbox for confirmation?) te
                return false;
            }
        });


        //call Methods onCreate
        loadStoredRelays();
        relayAdapter = new RVadapterRelay(listRelays);
        recyclerView.setAdapter(relayAdapter);


        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                        final int position = viewHolder.getAdapterPosition();

                        Snackbar snackbarDelete = Snackbar
                                .make(recyclerView, "Relay is deleted", Snackbar.LENGTH_LONG)
                                .setAction("UNDO", new View.OnClickListener(){

                                    @Override
                                    public void onClick(View v) {
                                        //restore the relay from the TempRelay object
                                        if (tempDeletedRelay != null){
                                            listRelays.add(position, tempDeletedRelay);
                                            recyclerView.getAdapter().notifyItemInserted(position);
                                        }

                                    }
                                });
                        snackbarDelete.setActionTextColor(Color.RED);
                        snackbarDelete.show();

                        //store relay in TempRelay object to undo the deletion
                        tempDeletedRelay = listRelays.get(position);

                        //delete the swiped recyclerView item
                        listRelays.remove(position);
                        recyclerView.getAdapter().notifyItemRemoved(position);
                        storeRelays();

                    }
        };

        //Attach the ItemTouchHelper to the Relay RecyclerView
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

    }




    private void storeRelays (){
        //TODO: store the listRelays for a specific device ID (so there aren't the same relay foreach device)
        SharedPreferences.Editor prefsEditor = relaySharedPref.edit();
        //transform the relay list to a string
        Gson gson = new Gson();
        String json = gson.toJson(listRelays);
        //store this Json string in Shared Preferences
        prefsEditor.putString("Saved Relays", json);
        prefsEditor.commit();

    }

    private void loadStoredRelays (){
        Gson gson = new Gson();
        //load the Json String. If no string is available the relay list is null
        String json = relaySharedPref.getString("Saved Relays", "");
        //transform the Json string into the original relay list
        listRelays = gson.fromJson(json, new TypeToken<List<Relay>>() {}.getType());

        if (listRelays == null){
            //create a new empty ArrayList
            listRelays = new ArrayList<>();
        }
    }



    private void toggleRelay(final Relay relay){

        relayIsSwitching = false;
        for (Relay relayInList: listRelays) {

            if (relayInList.tryToSwitch){
                relayIsSwitching = true;
            }

        }

        if (!relayIsSwitching){                 //only toggle if no other relay is currently switching

            relay.tryToSwitch = true;
            recyclerView.getAdapter().notifyDataSetChanged();

            Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Integer>() {

                @Override
                public Integer callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                    Integer success = 0;

                    //set the commandValue to the opposite of isSwitched
                    commandValue = (relay.isSwitched) ? "LOW" : "HIGH";
                    //the commands in functionCommandList will be executed by the Particle device
                    functionCommandList = new ArrayList<String>();
                    functionCommandList.add(relay.pin);
                    functionCommandList.add(commandValue);

                    particleCloud.getDevices();
                    myParticleDevice = particleCloud.getDevice(myParticleID);

                    try {
                        success = myParticleDevice.callFunction("toggleRelay",functionCommandList);

                    } catch (io.particle.android.sdk.cloud.ParticleDevice.FunctionDoesNotExistException e) {
                        Toaster.l(RelayScrollingActivity.this, "Function doesn't exist!");
                        relay.tryToSwitch = false;
                    }

                    return success;
                }


                public void onSuccess(Integer returnValue) {
                    if (returnValue == 1) {
                        relay.isSwitched = (commandValue.equals("HIGH"));
                        relay.tryToSwitch = false;
                        //recyclerView.getAdapter().notifyDataSetChanged();
                    }
                    else if (returnValue == -1){
                        relay.tryToSwitch = false;
                        //recyclerView.getAdapter().notifyDataSetChanged();
                        Toaster.l(RelayScrollingActivity.this, "Relay out of limit");
                    }
                    else if (returnValue == -2){
                        relay.tryToSwitch = false;
                        recyclerView.getAdapter().notifyDataSetChanged();
                        Toaster.l(RelayScrollingActivity.this, "Wrong command");
                    }

                    recyclerView.getAdapter().notifyDataSetChanged();
                    storeRelays();

                }

                public void onFailure(ParticleCloudException e) {
                    relay.tryToSwitch = false;
                    Log.e("SOME_TAG", e.getBestMessage());
                    Toaster.l(RelayScrollingActivity.this, e.getBestMessage());
                    recyclerView.getAdapter().notifyDataSetChanged();

                }


            });

        }
        else {
            Toaster.l(RelayScrollingActivity.this, "Wait till all relays finished switching");
        }


    }


    private void showConfirmationPopup(final Relay relay){
        new MaterialDialog.Builder(this)
                .title("Do you want to toggle this Relay?")
                .positiveText("Yes")
                .negativeText("No")
                .onPositive(new MaterialDialog.SingleButtonCallback() {

                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        toggleRelay(relay);
                    }
                })
                .show();

    }

    private void showAddNewRelayPopup(){
        boolean wrapInScrollView = true;

        final ArrayList<String> DOPinsSpinner = new ArrayList<String>();
        DOPinsSpinner.clear();

        //D2 Pin is used by the DHT22
        //TODO: Option in settings to use the D0 pin

        DOPinsSpinner.add("D0");
        DOPinsSpinner.add("D1");
        //DOPinsSpinner.add("D2");
        DOPinsSpinner.add("D3");
        DOPinsSpinner.add("D4");
        DOPinsSpinner.add("D5");
        DOPinsSpinner.add("D6");
        DOPinsSpinner.add("D7");

        if (listRelays!=null){
            //show only DO Pins which aren't used from other Relays
            for (Relay relay: listRelays)
            {
                DOPinsSpinner.remove(relay.pin.toString());
            }
        }


        MaterialDialog dialog = new MaterialDialog.Builder(RelayScrollingActivity.this)
                .title("Add new Relay")
                .customView(R.layout.add_new_relay_popup, wrapInScrollView)
                .positiveText("Yup")
                .negativeText("Nope")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                        EditText relayName = (EditText) dialog.getCustomView().findViewById(R.id.input_relay_name);
                        CheckBox confirmation = (CheckBox) dialog.getCustomView().findViewById(R.id.ConfirmationCheckBox);
                        editPin = pinSpinner.getSelectedItem().toString();
                        editRelayName = relayName.getText().toString();
                        editSwitchConfirmation = confirmation.isChecked();

                        listRelays.add(new Relay(editRelayName, editPin, false, editSwitchConfirmation, false));
                        recyclerView.getAdapter().notifyDataSetChanged();
                        storeRelays();
                    }
                })
                .show();


        //for later Access add "dialog.getCustomView()"
        //otherwise spinner is null, because the dialog isn't fully inflated
        pinSpinner = (Spinner) dialog.getCustomView().findViewById(R.id.relayPinSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(RelayScrollingActivity.this, android.R.layout.simple_spinner_item, DOPinsSpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pinSpinner.setAdapter(adapter);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }
}
