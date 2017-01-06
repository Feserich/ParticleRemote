package com.fese.particleremote;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.steamcrafted.loadtoast.LoadToast;


import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.IOException;
import java.util.ArrayList;

import io.particle.android.sdk.cloud.*;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;



public class TempHoneywellActivity extends AppCompatActivity {

    private String myParticleID;
    private ParticleDevice myParticleDevice;
    private TextSwitcher ts_targetTemp;
    private SharedPreferences honeywellTemperaturesValuesSharedPref;
    private int targetTemp = 0;
    private static final int LOWEST_TARGET_TEMP_VALUE = 13;             //the lowest temperature value on the seekbar. Honeywell is set to OFF
    private static final String HONEYWELL_OFF_VALUE = "75";
    private static int ComfortTemperatureValue = 22;
    private static int NightTemperatureValue = 18;
    private LoadToast setTempToast;
    private View viewHoneywell;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_honeywell);

        targetTemp = LOWEST_TARGET_TEMP_VALUE;

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        viewHoneywell = (RelativeLayout) findViewById(R.id.activity_temp_honeywell);

        honeywellTemperaturesValuesSharedPref = getPreferences(MODE_PRIVATE);

        Button btn_setTemp = (Button)findViewById(R.id.btn_setTargetTemp);
        ts_targetTemp = (TextSwitcher)findViewById(R.id.ts_targetTemp);

        Button btn_setTempOff = (Button)findViewById(R.id.btn_tempOff);
        Button btn_setTempComfort = (Button)findViewById(R.id.btn_tempComfort);
        Button btn_setTempNight = (Button)findViewById(R.id.btn_tempNight);

        setTempToast = new LoadToast(this);
        setTempToast.setText("Setting temperature...");
        //setTempToast.setTranslationY(200);                //translation in pixels

        btn_setTemp.setOnClickListener(setTempOnClick);
        btn_setTempOff.setOnClickListener(setTempOffOnClick);
        btn_setTempComfort.setOnClickListener(setTempComfortOnClick);
        btn_setTempNight.setOnClickListener(setTempNightOnClick);



        ts_targetTemp.setFactory(new ViewSwitcher.ViewFactory() {

            public View makeView() {
                // create new textView and set the properties like color, size etc
                TextView myText = new TextView(TempHoneywellActivity.this);
                myText.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                myText.setTextSize(92);
                myText.setText(String.valueOf(targetTemp));                             //TODO: set Unknown Temp or save last targetTemp
                //myText.setTextColor(Color.BLUE);
                return myText;
            }
        });


        DiscreteSeekBar discreteSeekBarTargetTemp = (DiscreteSeekBar) findViewById(R.id.discrete_seekbar_target_temp);
        discreteSeekBarTargetTemp.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
            @Override
            public int transform(int value) {
                targetTemp = value;
                setTextSwitchTemperature(value);
                
                return value;
            }
        });


        //call Method on Create
        ParticleCloudSDK.init(this);
        getParticleDeviceInstance();
        loadValueSettings();

    }




    private void setTemperature(final String targetTempRAW){
        if (myParticleDevice != null){

            setTempToast.show();
            Async.executeAsync(myParticleDevice, new Async.ApiWork<io.particle.android.sdk.cloud.ParticleDevice, Integer>() {

                @Override
                public Integer callApi(io.particle.android.sdk.cloud.ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                    Integer success = 0;

                    //the commands in functionCommandList will be executed by the Particle device
                    ArrayList functionCommandList = new ArrayList<String>();
                    functionCommandList.add(targetTempRAW);

                    try {
                        success = particleDevice.callFunction("setTempHoney",functionCommandList);

                    } catch (io.particle.android.sdk.cloud.ParticleDevice.FunctionDoesNotExistException e) {
                        Snackbar snackbarError = Snackbar
                                .make(viewHoneywell, e.getMessage().toString(), Snackbar.LENGTH_LONG);
                        snackbarError.show();

                    }

                    return success;
                }


                public void onSuccess(Integer returnValue) {

                    switch (returnValue){
                        case 1:
                            setTempToast.success();
                            break;
                        case -1:
                            Snackbar snackbarError1 = Snackbar
                                    .make(viewHoneywell, "Wrong response!", Snackbar.LENGTH_LONG);
                            snackbarError1.show();
                            setTempToast.error();
                            break;
                        case -2:
                            Snackbar snackbarError2 = Snackbar
                                    .make(viewHoneywell, "Read Buffer Overflow!", Snackbar.LENGTH_LONG);
                            snackbarError2.show();
                            setTempToast.error();
                            break;
                        case -3:
                            Snackbar snackbarError3 = Snackbar
                                    .make(viewHoneywell, "TimeOut!", Snackbar.LENGTH_LONG);
                            snackbarError3.show();
                            setTempToast.error();
                            break;
                        default:
                            setTempToast.error();
                            break;
                    }

                    //recyclerView.getAdapter().notifyDataSetChanged();
                }

                public void onFailure(ParticleCloudException e) {
                    Log.e("SOME_TAG", e.getBestMessage());
                    Snackbar snackbarError = Snackbar
                            .make(viewHoneywell, e.getBestMessage(), Snackbar.LENGTH_LONG);
                    snackbarError.show();
                    setTempToast.error();

                }

            });
        }
        else {
            Snackbar snackbarWait = Snackbar
                    .make(viewHoneywell, "Wait a sec!", Snackbar.LENGTH_LONG);
            snackbarWait.show();
        }



    }

    private void setTextSwitchTemperature (int temperatureValue) {

        switch (temperatureValue){
            case LOWEST_TARGET_TEMP_VALUE: ts_targetTemp.setText("OFF");
                break;

            case 27: ts_targetTemp.setText("ON");
                break;

            default: ts_targetTemp.setText(String.valueOf(temperatureValue) + " °C");
                break;

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
                Snackbar snackbarError = Snackbar
                        .make(viewHoneywell, e.getBestMessage(), Snackbar.LENGTH_LONG);
                snackbarError.show();
            }

        });

    }

    //TODO: Edit temperatures for comfort and night mode in settings

    private void storeValueSettings() {
        SharedPreferences.Editor prefsEditor = honeywellTemperaturesValuesSharedPref.edit();
        prefsEditor.putInt("Night Mode Temperature Value", NightTemperatureValue);
        prefsEditor.putInt("Comfort Temperature Value", ComfortTemperatureValue);
        prefsEditor.apply();
    }

    private void loadValueSettings() {
        Integer night = honeywellTemperaturesValuesSharedPref.getInt("Night Mode Temperature Value", -1);
        Integer comfort = honeywellTemperaturesValuesSharedPref.getInt("Comfort Temperature Value", -1);

        NightTemperatureValue = (night == -1) ? NightTemperatureValue : night;
        ComfortTemperatureValue = (comfort == -1) ? ComfortTemperatureValue : comfort;
    }



    View.OnClickListener setTempOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            //float targetTemp_f = Float.valueOf();
            //int targetTempRaw = Math.round(targetTemp_f * 10);
            if (targetTemp == LOWEST_TARGET_TEMP_VALUE) {
                setTemperature(HONEYWELL_OFF_VALUE);                                   //value to set Honeywell to OFF
            }
            else {
                setTemperature(String.valueOf(targetTemp * 10));
            }


        }

    };

    View.OnClickListener setTempOffOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            setTemperature(HONEYWELL_OFF_VALUE);
            setTextSwitchTemperature(LOWEST_TARGET_TEMP_VALUE);                         //value to set Honeywell to OFF
        }

    };

    View.OnClickListener setTempComfortOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            setTemperature(String.valueOf(ComfortTemperatureValue * 10));               //multiple 10 for RAW value
            setTextSwitchTemperature(ComfortTemperatureValue);
        }

    };

    View.OnClickListener setTempNightOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            setTemperature(String.valueOf(NightTemperatureValue * 10));                 //multiple 10 for RAW value
            setTextSwitchTemperature(NightTemperatureValue);
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_temp_honeywell, menu);
        return super.onCreateOptionsMenu(menu);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_settings:
                //TODO: show settings
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
