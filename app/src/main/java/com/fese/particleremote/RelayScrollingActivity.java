package com.fese.particleremote;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class RelayScrollingActivity extends AppCompatActivity {

    CollapsingToolbarLayout collapsingToolbar;
    private String myPhotonID = null;
    private RecyclerView recyclerView;
    RVadapterRelay relayAdapter;
    private List<Relay> listRelays;
    private String editPin;
    private String editRelayName;
    private Spinner pinSpinner;
    private SharedPreferences relaySharedPref;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relay_scrolling);
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

        //get the ID of the selected Particle Device from the MainActivity
        myPhotonID = (String) getIntent().getSerializableExtra("deviceID");



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
                //TODO: toggle Relay

            }
        });

        RVadapterRelay.RelayViewHolder.setOnItemLongClickListener(new RVadapterRelay.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(String pin) {
                //TODO: show relay properties (edit name, edit pin (dropdown menu), checkbox for confirmation?)
                return false;
            }
        });





        //call Methods onCreate
        loadStoredRelays();
        initializeAdapter();

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                        int position = viewHolder.getAdapterPosition();

                        //TODO: show Snackbar with undo delete
                        //Snackbar.make(viewHolder, "Replace with your own action", Snackbar.LENGTH_LONG)
                        //        .setAction("Action", null).show();

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

    private void initializeAdapter(){
            relayAdapter = new RVadapterRelay(listRelays);
            recyclerView.setAdapter(relayAdapter);

    }

    private void storeRelays (){
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
        String json = relaySharedPref.getString("Saved Relays","");
        //transform the Json string into the original relay list
        listRelays = gson.fromJson(json, new TypeToken<List<Relay>>() {}.getType());

        if (listRelays == null){
            //create a new empty ArrayList
            listRelays = new ArrayList<>();
        }


    }

    private void showAddNewRelayPopup(){
        boolean wrapInScrollView = true;

        final ArrayList<String> DOPinsSpinner = new ArrayList<String>();
        DOPinsSpinner.clear();

        DOPinsSpinner.add("D0");
        DOPinsSpinner.add("D1");
        DOPinsSpinner.add("D2");
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
                        editPin = pinSpinner.getSelectedItem().toString();
                        editRelayName = relayName.getText().toString();

                        listRelays.add(new Relay(editRelayName, editPin, false));

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
}
