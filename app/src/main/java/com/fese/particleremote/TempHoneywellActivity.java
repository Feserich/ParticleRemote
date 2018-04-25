package com.fese.particleremote;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import net.steamcrafted.loadtoast.LoadToast;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.IOException;
import java.util.ArrayList;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;



public class TempHoneywellActivity extends AppCompatActivity {

    private String myParticleID;
    private ParticleDevice myParticleDevice;
    private TextSwitcher ts_targetTemp;
    private SharedPreferences SharedPref;
    private String targetTemp;
    private LoadToast setTempToast;
    private View viewHoneywell;
    private DiscreteSeekBar discreteSeekBarTargetTemp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_honeywell);

        targetTemp = getString(R.string.LOWEST_TEMPERATURE_SELECTION_VALUE);

        Toolbar toolbar = findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        viewHoneywell = findViewById(R.id.activity_temp_honeywell);

        SharedPref  = PreferenceManager.getDefaultSharedPreferences(this);

        Button btn_setTemp = findViewById(R.id.btn_setTargetTemp);
        ts_targetTemp = findViewById(R.id.ts_targetTemp);

        Button btn_setTempOff = findViewById(R.id.btn_tempOff);
        Button btn_setTempComfort = findViewById(R.id.btn_tempComfort);
        Button btn_setTempNight = findViewById(R.id.btn_tempNight);

        setTempToast = new LoadToast(this);
        setTempToast.setText(getString(R.string.setting_temperature_toast_text));
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
                myText.setText(getString(R.string.LOWEST_TEMPERATURE_SELECTION_VALUE));
                //myText.setTextColor(Color.BLUE);
                return myText;
            }
        });


        discreteSeekBarTargetTemp = findViewById(R.id.discrete_seekbar_target_temp);

        discreteSeekBarTargetTemp.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
            @Override
            public int transform(int value) {
                targetTemp = String.valueOf(value);
                setTextSwitchTemperature(targetTemp);
                
                return value;
            }
        });

        discreteSeekBarTargetTemp.setMin(Integer.valueOf(getString(R.string.LOWEST_TEMPERATURE_SELECTION_VALUE)));
        discreteSeekBarTargetTemp.setMax(Integer.valueOf(getString(R.string.HIGHEST_TEMPERATURE_SELECTION_VALUE)));




        //call Method on Create
        ParticleCloudSDK.init(this);
        getParticleDeviceInstance();
        setTextSwitchTemperature(getString(R.string.LOWEST_TEMPERATURE_SELECTION_VALUE));

    }




    private void setTemperature(final String targetTempRAW){
        if (myParticleDevice != null){

            setTempToast.show();
            Async.executeAsync(myParticleDevice, new Async.ApiWork<io.particle.android.sdk.cloud.ParticleDevice, Integer>() {

                @Override
                public Integer callApi(io.particle.android.sdk.cloud.ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                    Integer success = 0;

                    //the commands in functionCommandList will be executed by the Particle device
                    ArrayList<String> functionCommandList = new ArrayList<>();
                    functionCommandList.add(targetTempRAW);

                    try {
                        success = particleDevice.callFunction("setTempHoney",functionCommandList);

                    } catch (io.particle.android.sdk.cloud.ParticleDevice.FunctionDoesNotExistException e) {
                        Snackbar snackbarError = Snackbar
                                .make(viewHoneywell, e.getMessage(), Snackbar.LENGTH_LONG);
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
                                    .make(viewHoneywell, getString(R.string.wrong_response), Snackbar.LENGTH_LONG);
                            snackbarError1.show();
                            setTempToast.error();
                            break;
                        case -2:
                            Snackbar snackbarError2 = Snackbar
                                    .make(viewHoneywell, getString(R.string.read_buffer_overflow), Snackbar.LENGTH_LONG);
                            snackbarError2.show();
                            setTempToast.error();
                            break;
                        case -3:
                            Snackbar snackbarError3 = Snackbar
                                    .make(viewHoneywell, getString(R.string.time_out), Snackbar.LENGTH_LONG);
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

    private void setTextSwitchTemperature (String temperatureValue) {

        if (temperatureValue.equals(getString(R.string.LOWEST_TEMPERATURE_SELECTION_VALUE))){
            ts_targetTemp.setText(getString(R.string.off));
        }
        else if (temperatureValue.equals(getString(R.string.HIGHEST_TEMPERATURE_SELECTION_VALUE))) {
            ts_targetTemp.setText(getString(R.string.on));
        }
        else {
            ts_targetTemp.setText(String.valueOf(temperatureValue) + " °C");
        }


    }

    private void getParticleDeviceInstance(){

        //get the ID of the selected Particle Device from the MainActivity
        myParticleID = (String) getIntent().getSerializableExtra("deviceID");

        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Void>() {

            public Void callApi(ParticleCloud particleCloud) throws ParticleCloudException {
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



    View.OnClickListener setTempOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            //float targetTemp_f = Float.valueOf(targetTemp);
            //int targetTempRaw = Math.round(targetTemp_f * 10);

            if (targetTemp.equals(getString(R.string.LOWEST_TEMPERATURE_SELECTION_VALUE))) {
                setTemperature(getString(R.string.HONEYWELL_OFF_VALUE));                                   //value to set Honeywell to OFF
            }
            else {
                setTemperature(targetTemp + "0");                                                          //multiple 10 for RAW value == add a zero
            }


        }

    };




    View.OnClickListener setTempOffOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            String lowestValue = getString(R.string.LOWEST_TEMPERATURE_SELECTION_VALUE);
            setTemperature(getString(R.string.HONEYWELL_OFF_VALUE));
            setTextSwitchTemperature(lowestValue);                         //value to set Honeywell to OFF
            discreteSeekBarTargetTemp.setProgress(Integer.valueOf(lowestValue));
        }

    };

    View.OnClickListener setTempComfortOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            String comfortValue = SharedPref.getString(getString(R.string.pref_comfort_key), "");
            setTemperature(comfortValue + "0");               //multiple 10 for RAW value == add a zero
            setTextSwitchTemperature(comfortValue);
            discreteSeekBarTargetTemp.setProgress(Integer.valueOf(comfortValue));

        }

    };

    View.OnClickListener setTempNightOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            String nightValue = SharedPref.getString(getString(R.string.pref_night_key), "");
            setTemperature(nightValue + "0");                 //multiple 10 for RAW value == add a zero
            setTextSwitchTemperature(nightValue);
            discreteSeekBarTargetTemp.setProgress(Integer.valueOf(nightValue));
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
                Intent intentSettings = new Intent(TempHoneywellActivity.this, UserSettingsActivity.class);
                TempHoneywellActivity.this.startActivity(intentSettings);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
