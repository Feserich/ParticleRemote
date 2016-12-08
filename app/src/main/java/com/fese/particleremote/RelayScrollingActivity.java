package com.fese.particleremote;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.text.TextUtilsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relay_scrolling);

        //call Methods onCreate
        Log.d(TAG,"Start onCreate");
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

                    //set the commandValue to the opposite of isSwitched
                    commandValue = (relay.isSwitched) ? "LOW " : "HIGH";
                    //the commands in functionCommandList will be executed by the Particle device
                    functionCommandList = new ArrayList<String>();
                    functionCommandList.add(relay.pin);
                    functionCommandList.add(commandValue);
                    functionCommandList.add(relay.toggleTime.toString());

                    try {
                        Log.d(TAG,"Call Particle Function");
                        success = particleDevice.callFunction("toggleRelay",functionCommandList);

                    } catch (io.particle.android.sdk.cloud.ParticleDevice.FunctionDoesNotExistException e) {
                        Toaster.l(RelayScrollingActivity.this, "Function doesn't exist!");
                        relay.tryToSwitch = false;
                    }

                    return success;
                }


                public void onSuccess(Integer returnValue) {
                    if (returnValue == 1) {
                        if (relay.toggleTime == 0){
                            relay.isSwitched = (commandValue.equals("HIGH"));       //only change isSwitched if there is no toggle time
                        }
                        relay.tryToSwitch = false;



                    }
                    else if (returnValue == -1){
                        relay.tryToSwitch = false;
                        Toaster.l(RelayScrollingActivity.this, "Relay out of limit");
                    }
                    else if (returnValue == -2){
                        relay.tryToSwitch = false;
                        Toaster.l(RelayScrollingActivity.this, "Wrong command");
                    }

                    recyclerView.getAdapter().notifyDataSetChanged();
                    storeRelays();
                    Log.d(TAG,"End of Particle Function");
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
            Toaster.l(RelayScrollingActivity.this, "Wait a sec!");
        }


    }

    private void getParticleDeviceInstance(){

        //get the ID of the selected Particle Device from the MainActivity
        myParticleID = (String) getIntent().getSerializableExtra("deviceID");

        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Void>() {

            public Void callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                myParticleDevice = ParticleCloudSDK.getCloud().getDevice(myParticleID);
                return null;
            }

            public void onSuccess(Void aVoid) {

            }

            public void onFailure(ParticleCloudException e) {
                Log.e("SOME_TAG", e.getBestMessage());
                Toaster.l(RelayScrollingActivity.this, e.getBestMessage());
            }

        });

    }

    private void storeRelays (){
        SharedPreferences.Editor prefsEditor = relaySharedPref.edit();
        //transform the relay list to a string
        Gson gson = new Gson();
        String json = gson.toJson(listRelays);
        //store this Json string in Shared Preferences
        prefsEditor.putString("Saved Relays for Particle Device:" + myParticleID, json);
        prefsEditor.commit();

    }

    private void loadStoredRelays (){
        Gson gson = new Gson();
        //load the Json String. If no string is available the relay list is null
        String json = relaySharedPref.getString("Saved Relays for Particle Device:" + myParticleID, "");
        //transform the Json string into the original relay list
        listRelays = gson.fromJson(json, new TypeToken<List<Relay>>() {}.getType());

        if (listRelays == null){
            //create a new empty ArrayList
            listRelays = new ArrayList<>();
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

        //Initialize Variables
        boolean wrapInScrollView = true;
        final ArrayList<String> Arr_TimeUnitSpinner = new ArrayList<String>();
        Arr_TimeUnitSpinner.clear();
        Arr_TimeUnitSpinner.add("sec");
        Arr_TimeUnitSpinner.add("min");
        Arr_TimeUnitSpinner.add("h");

        //TODO: Option in settings to use the D2 pin
        final ArrayList<String> Arr_DOPinsSpinner = new ArrayList<String>();
        Arr_DOPinsSpinner.clear();
        Arr_DOPinsSpinner.add("D0");
        Arr_DOPinsSpinner.add("D1");
        //Arr_DOPinsSpinner.add("D2");              //D2 Pin is used by the DHT22
        Arr_DOPinsSpinner.add("D3");
        Arr_DOPinsSpinner.add("D4");
        Arr_DOPinsSpinner.add("D5");
        Arr_DOPinsSpinner.add("D6");
        Arr_DOPinsSpinner.add("D7");


        if (listRelays!=null){                      //show only DO Pins which aren't used from other Relays
            for (Relay relay: listRelays)
            {
                Arr_DOPinsSpinner.remove(relay.pin.toString());
            }
        }


        //TODO: CheckBox: Relay is Switched on LOW Output (negate relay.isSwitched)
        //Show Popup Dialog
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
                        String editPin = pinSpinner.getSelectedItem().toString();
                        checkBoxTimeUnit = (CheckBox) dialog.getCustomView().findViewById(R.id.relayTimerCheckBox);
                        et_toggleTime = (EditText) dialog.getCustomView().findViewById(R.id.inputTimerNumber);
                        editRelayName = relayName.getText().toString();
                        editSwitchConfirmation = confirmation.isChecked();

                        if (checkBoxTimeUnit.isChecked() && !et_toggleTime.getText().toString().isEmpty())
                        {

                            toggleTime = Integer.parseInt(et_toggleTime.getText().toString());
                            switch (SpinnerTimeUnit.getSelectedItem().toString()) {
                                case "s":
                                    break;
                                case "min":
                                    toggleTime = toggleTime * 60;
                                    break;
                                case "h":
                                    toggleTime = toggleTime * 60 * 60;
                                    break;
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
        ArrayAdapter<String> PinSpinnerAdapter = new ArrayAdapter<String>(RelayScrollingActivity.this, android.R.layout.simple_spinner_item, Arr_DOPinsSpinner);
        PinSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pinSpinner.setAdapter(PinSpinnerAdapter);

        SpinnerTimeUnit = (Spinner) dialog.getCustomView().findViewById(R.id.relayTimerUnitSpinner);
        ArrayAdapter<String> TimeUnitSpinnerAdapter = new ArrayAdapter<String>(RelayScrollingActivity.this, android.R.layout.simple_spinner_item, Arr_TimeUnitSpinner);
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
        collapsingToolbar.setTitle("Toggle Relay");

        relaySharedPref = getPreferences(MODE_PRIVATE);

        recyclerView = (RecyclerView) findViewById(R.id.relayList);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);



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
                                .make(recyclerView, "Relay has been deleted", Snackbar.LENGTH_LONG)
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
