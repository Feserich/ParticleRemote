package com.fese.particleremote;

import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import net.steamcrafted.loadtoast.LoadToast;


import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.IOException;
import java.util.ArrayList;

import io.particle.android.sdk.cloud.*;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class TempHoneywellActivity extends AppCompatActivity {

    private String myParticleID;
    private ParticleDevice myParticleDevice;
    private Button btn_setTemp;
    private TextSwitcher ts_targetTemp;
    private int targetTemp = 0;
    public static final int LOWEST_TARGET_TEMP_VALUE = 13;
    private LoadToast setTempToast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_honeywell);

        targetTemp = LOWEST_TARGET_TEMP_VALUE;

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);



        btn_setTemp = (Button)findViewById(R.id.btn_setTargetTemp);
        ts_targetTemp = (TextSwitcher)findViewById(R.id.ts_targetTemp);

        setTempToast = new LoadToast(this);
        setTempToast.setText("Setting temperature...");
        //setTempToast.setTranslationY(200);                //translation in pixels

        btn_setTemp.setOnClickListener(setTempOnClick);

        ts_targetTemp.setFactory(new ViewSwitcher.ViewFactory() {

            public View makeView() {
                // create new textView and set the properties like color, size etc
                TextView myText = new TextView(TempHoneywellActivity.this);
                myText.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                myText.setTextSize(92);
                myText.setText(String.valueOf(targetTemp));
                //myText.setTextColor(Color.BLUE);
                return myText;
            }
        });


        DiscreteSeekBar discreteSeekBarTargetTemp = (DiscreteSeekBar) findViewById(R.id.discrete_seekbar_target_temp);
        discreteSeekBarTargetTemp.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
            @Override
            public int transform(int value) {
                targetTemp = value;
                switch (targetTemp){
                    case 13: ts_targetTemp.setText("OFF");
                        break;

                    case 27: ts_targetTemp.setText("ON");
                        break;

                    default: ts_targetTemp.setText(String.valueOf(targetTemp) + " °C");
                        break;

                }
                
                return value;
            }
        });




        getParticleDeviceInstance();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    View.OnClickListener setTempOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            //float targetTemp_f = Float.valueOf();
            //int targetTempRaw = Math.round(targetTemp_f * 10);
            if (targetTemp == LOWEST_TARGET_TEMP_VALUE) {
                setTemperature("75");                                   //value to set Honeywell to OFF
            }
            else {
                setTemperature(String.valueOf(targetTemp * 10));
            }


        }

        };

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
                        Toaster.l(TempHoneywellActivity.this, e.toString());
                        setTempToast.error();
                    }

                    return success;
                }


                public void onSuccess(Integer returnValue) {
                    switch (returnValue){
                        case 1:
                            setTempToast.success();
                            break;
                        case -1:
                            setTempToast.error();
                            Toaster.l(TempHoneywellActivity.this, "Wrong response!");
                            break;
                        case -2:
                            Toaster.l(TempHoneywellActivity.this, "Read Buffer Overflow!");
                            setTempToast.error();
                            break;
                        case -3:
                            Toaster.l(TempHoneywellActivity.this, "TimeOut!");
                            setTempToast.error();
                            break;
                    }

                    //recyclerView.getAdapter().notifyDataSetChanged();
                }

                public void onFailure(ParticleCloudException e) {
                    Log.e("SOME_TAG", e.getBestMessage());
                    setTempToast.error();
                    Toaster.l(TempHoneywellActivity.this, e.getBestMessage());
                }

            });
        }
        else {
            Toaster.l(TempHoneywellActivity.this, "Wait a sec!");
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
                Toaster.l(TempHoneywellActivity.this, e.getBestMessage());
            }

        });

    }

}
