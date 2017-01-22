package com.fese.particleremote;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import io.particle.android.sdk.cloud.*;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;


public class RelayScrollingActivity extends AppCompatActivity {

    CollapsingToolbarLayout collapsingToolbar;
    private String myParticleID = null;
    private RecyclerView recyclerView;
    RVadapterRelay relayAdapter;
    private List<Relay> listRelays;
    private Relay tempDeletedRelay;

    private String editRelayName;
    private boolean editSwitchConfirmation;
    private Spinner pinSpinner;
    private Spinner SpinnerTimeUnit;
    private CheckBox checkBoxTimeUnit;
    private EditText et_toggleTime;
    private SharedPreferences relaySharedPref;
    private ParticleDevice myParticleDevice;
    private List<String> functionCommandList;
    private String commandValue;
    private Integer toggleTime;
    private static final String TAG = "RelayScrollingActivity";
    private Boolean switchedOnLowOutput = true;
    private static final String lowCommand = "LOW ";                //add a Space to the "LOW" command, so that particle device can interpret toggleTime (same count of character as "HIGH")
    private static final String highCommand = "HIGH";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relay_scrolling);

        //call Methods onCreate
        Log.d(TAG,"Start onCreate");
        ParticleCloudSDK.init(this);
        initializeRelayScrollingActivityAndOnClickListener();
        getParticleDeviceInstance();
        loadStoredRelays();
        relayAdapter = new RVadapterRelay(listRelays);
        recyclerView.setAdapter(relayAdapter);
        Log.d(TAG,"Finished onCreate");

    }

    private void toggleRelay(final Relay relay){

        if (myParticleDevice != null){
            relay.tryToSwitch = true;
            recyclerView.getAdapter().notifyDataSetChanged();

            Async.executeAsync(myParticleDevice, new Async.ApiWork<ParticleDevice, Integer>() {

                @Override
                public Integer callApi(ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                    Integer success = 0;

                    if (switchedOnLowOutput){
                        //relay is switched on low output
                        commandValue = (relay.isSwitched) ? highCommand : lowCommand;
                    }
                    else {
                        //relay is switched on high output
                        commandValue = (relay.isSwitched) ? lowCommand : highCommand;
                    }

                    //the commands in functionCommandList will be executed by the Particle device
                    functionCommandList = new ArrayList<>();
                    functionCommandList.add(relay.pin);
                    functionCommandList.add(commandValue);
                    functionCommandList.add(relay.toggleTime.toString());

                    try {
                        Log.d(TAG,"Call Particle Function");
                        success = particleDevice.callFunction("toggleRelay",functionCommandList);

                    } catch (io.particle.android.sdk.cloud.ParticleDevice.FunctionDoesNotExistException e) {
                        Snackbar snackbarError = Snackbar
                                .make(recyclerView, e.getMessage(), Snackbar.LENGTH_LONG);
                        snackbarError.show();
                        relay.tryToSwitch = false;
                    }

                    return success;
                }


                public void onSuccess(Integer returnValue) {
                    if (returnValue == 1) {

                        //only change isSwitched if there is no toggle time
                        if (relay.toggleTime == 0){
                            relay.isSwitched = (switchedOnLowOutput) ? (commandValue.equals(lowCommand)) :(commandValue.equals(highCommand));
                        }
                        relay.tryToSwitch = false;

                    }
                    else if (returnValue == -1){
                        relay.tryToSwitch = false;
                        Snackbar snackbarError = Snackbar
                                .make(recyclerView, R.string.relay_out_of_limit, Snackbar.LENGTH_LONG);
                        snackbarError.show();
                    }
                    else if (returnValue == -2){
                        relay.tryToSwitch = false;
                        Snackbar snackbarError = Snackbar
                                .make(recyclerView, R.string.wrong_command, Snackbar.LENGTH_LONG);
                        snackbarError.show();
                    }
                    else if (returnValue == -3){
                        relay.tryToSwitch = false;
                        Snackbar snackbarError = Snackbar
                                .make(recyclerView, R.string.pin_used_by_DHT22_error_message, Snackbar.LENGTH_LONG);
                        snackbarError.show();
                    }
                    else {
                        relay.tryToSwitch = false;
                    }


                    recyclerView.getAdapter().notifyDataSetChanged();
                    storeRelays();
                    Log.d(TAG,"End of Particle Function");
                }

                public void onFailure(ParticleCloudException e) {
                    relay.tryToSwitch = false;
                    Log.e("SOME_TAG", e.getBestMessage());
                    Snackbar snackbarError = Snackbar
                            .make(recyclerView, e.getBestMessage(), Snackbar.LENGTH_LONG);
                    snackbarError.show();
                    recyclerView.getAdapter().notifyDataSetChanged();
                }


            });
        }
        else {
            Snackbar snackbarWait = Snackbar
                    .make(recyclerView, R.string.wait_a_sec, Snackbar.LENGTH_LONG);
            snackbarWait.show();
        }


    }

    private void getParticleDeviceInstance(){

        //get the ID of the selected Particle Device from the MainActivity
        myParticleID = (String) getIntent().getSerializableExtra("deviceID");

        //myParticleDevice = MyParticleDevice.getParticleDeviceInstance(myParticleID);


        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Void>() {

            public Void callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                myParticleDevice = ParticleCloudSDK.getCloud().getDevice(myParticleID);
                return null;
            }

            public void onSuccess(Void aVoid) {

            }

            public void onFailure(ParticleCloudException e) {
                Log.e("SOME_TAG", e.getBestMessage());
                Snackbar snackbarError = Snackbar
                        .make(recyclerView, e.getBestMessage(), Snackbar.LENGTH_LONG);
                snackbarError.show();
            }

        });



    }

    private void storeRelays (){
        SharedPreferences.Editor prefsEditor = relaySharedPref.edit();
        //transform the relay list to a string
        Gson gson = new Gson();
        String json = gson.toJson(listRelays);
        //store this Json string in Shared Preferences
        prefsEditor.putString(getString(R.string.saved_relay_shared_pref_key) + myParticleID, json);
        prefsEditor.apply();

    }

    private void loadStoredRelays (){
        Gson gson = new Gson();
        //load the Json String. If no string is available the relay list is null
        String json = relaySharedPref.getString(getString(R.string.saved_relay_shared_pref_key) + myParticleID, "");
        //transform the Json string into the original relay list
        listRelays = gson.fromJson(json, new TypeToken<List<Relay>>() {}.getType());

        if (listRelays == null){
            //create a new empty ArrayList
            listRelays = new ArrayList<>();
        }
    }

    private void showConfirmationPopup(final Relay relay){
        new MaterialDialog.Builder(this)
                .title(R.string.relay_toggle_confirmation_title)
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .onPositive(new MaterialDialog.SingleButtonCallback() {

                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        toggleRelay(relay);
                    }
                })
                .show();

    }

    private void showAddNewRelayPopup(){

        //Initialize Variables
        boolean wrapInScrollView = true;
        final ArrayList<String> Arr_TimeUnitSpinner = new ArrayList<>();
        Arr_TimeUnitSpinner.clear();
        Arr_TimeUnitSpinner.add(getString(R.string.second_short));
        Arr_TimeUnitSpinner.add(getString(R.string.minute_short));
        Arr_TimeUnitSpinner.add(getString(R.string.hour_short));





        List<String> Arr_PinsSpinner = new LinkedList<>(Arrays.asList(getResources().getStringArray(R.array.DigitalPins)));
        List<String> Arr_AOPinsSpinner = new LinkedList<>(Arrays.asList(getResources().getStringArray(R.array.AnalogPins)));

        for (String analogPin : Arr_AOPinsSpinner) {
            Arr_PinsSpinner.add(analogPin);
        }


        //remove the pin which is used by the DHT22 sensor
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String pinDHT22 = sharedPref.getString(getString(R.string.pref_DHT_input_pin_key), "");
        Arr_PinsSpinner.remove(pinDHT22);




        if (listRelays!=null){                      //show only DO Pins which aren't used from other Relays
            for (Relay relay: listRelays)
            {
                Arr_PinsSpinner.remove(relay.pin);
            }
        }


        //Show Popup Dialog
        MaterialDialog dialog = new MaterialDialog.Builder(RelayScrollingActivity.this)
                .title(R.string.add_relay_popup_title)
                .customView(R.layout.add_new_relay_popup, wrapInScrollView)
                .positiveText(R.string.yup)
                .negativeText(R.string.nope)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                        EditText relayName = (EditText) dialog.getCustomView().findViewById(R.id.input_relay_name);
                        CheckBox confirmation = (CheckBox) dialog.getCustomView().findViewById(R.id.ConfirmationCheckBox);
                        String editPin = pinSpinner.getSelectedItem().toString();
                        checkBoxTimeUnit = (CheckBox) dialog.getCustomView().findViewById(R.id.relayTimerCheckBox);
                        et_toggleTime = (EditText) dialog.getCustomView().findViewById(R.id.inputTimerNumber);
                        editRelayName = relayName.getText().toString();
                        editSwitchConfirmation = confirmation.isChecked();

                        if (checkBoxTimeUnit.isChecked() && !et_toggleTime.getText().toString().isEmpty())
                        {

                            toggleTime = Integer.parseInt(et_toggleTime.getText().toString());

                            if (SpinnerTimeUnit.getSelectedItem().toString().equals(getString(R.string.minute_short))){
                                toggleTime = toggleTime * 60;
                            }
                            else if (SpinnerTimeUnit.getSelectedItem().toString().equals(getString(R.string.hour_short))){
                                toggleTime = toggleTime * 60 * 60;
                            }

                        }
                        else{
                            toggleTime = 0;
                        }

                        listRelays.add(new Relay(editRelayName, editPin, false, editSwitchConfirmation, false, toggleTime));
                        recyclerView.getAdapter().notifyDataSetChanged();
                        storeRelays();
                    }
                })
                .show();


        //for later Access add "dialog.getCustomView()"
        //otherwise spinner is null, because the dialog isn't fully inflated

        //set the items from the Array above into the Spinner
        pinSpinner = (Spinner) dialog.getCustomView().findViewById(R.id.relayPinSpinner);
        ArrayAdapter<String> PinSpinnerAdapter = new ArrayAdapter<>(RelayScrollingActivity.this, android.R.layout.simple_spinner_item, Arr_PinsSpinner);
        PinSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pinSpinner.setAdapter(PinSpinnerAdapter);

        SpinnerTimeUnit = (Spinner) dialog.getCustomView().findViewById(R.id.relayTimerUnitSpinner);
        ArrayAdapter<String> TimeUnitSpinnerAdapter = new ArrayAdapter<>(RelayScrollingActivity.this, android.R.layout.simple_spinner_item, Arr_TimeUnitSpinner);
        TimeUnitSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerTimeUnit.setAdapter(TimeUnitSpinnerAdapter);
        SpinnerTimeUnit.setEnabled(false);


        checkBoxTimeUnit = (CheckBox) dialog.getCustomView().findViewById(R.id.relayTimerCheckBox);
        et_toggleTime = (EditText) dialog.getCustomView().findViewById(R.id.inputTimerNumber);

        //enable toggleTime EditText and SpinnerTimeUnit only if checkbox isChecked
        checkBoxTimeUnit.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    if (((CheckBox) v).isChecked()) {
                        //et_toggleTime.setFocusable(true);
                        et_toggleTime.setEnabled(true);
                        SpinnerTimeUnit.setEnabled(true);
                    } else {
                        //et_toggleTime.setFocusable(false);
                        et_toggleTime.setEnabled(false);
                        SpinnerTimeUnit.setEnabled(false);
                    }
                }
            });

    }

    private void initializeRelayScrollingActivityAndOnClickListener (){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle("Toggle Relay");
        }

        relaySharedPref = getPreferences(MODE_PRIVATE);

        recyclerView = (RecyclerView) findViewById(R.id.relayList);
        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_newRelay);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showAddNewRelayPopup();
                }
            });
        }


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
                //TODO: show relay properties (edit name, edit pin (dropdown menu), checkbox for confirmation?).
                return false;
            }
        });


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
                                .make(recyclerView, R.string.delete_relay_snackbar_text, Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo, new View.OnClickListener(){

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_relay_scrolling, menu);
        return super.onCreateOptionsMenu(menu);

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

            case R.id.menu_settings:
                Intent intentSettings = new Intent(RelayScrollingActivity.this, UserSettingsActivity.class);
                RelayScrollingActivity.this.startActivity(intentSettings);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }

    @Override
    protected void onResume()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        switchedOnLowOutput = sharedPref.getBoolean(getString(R.string.pref_toggle_relay_low_output_key), true);
        super.onResume();
    }



}
