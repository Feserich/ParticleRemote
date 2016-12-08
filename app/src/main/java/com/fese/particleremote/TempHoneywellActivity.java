package com.fese.particleremote;

import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
    private EditText et_targetTemp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_honeywell);

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);



        btn_setTemp = (Button)findViewById(R.id.btn_setTargetTemp);
        et_targetTemp = (EditText)findViewById(R.id.et_targetTemperature);

        btn_setTemp.setOnClickListener(setTempOnClick);




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
    //TODO: send Off Command (targetTempRaw = 75)

    View.OnClickListener setTempOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            float targetTemp = Float.valueOf(et_targetTemp.getText().toString());
            int targetTempRaw = Math.round(targetTemp * 10);
            setTemperature(Float.toString(targetTempRaw));

        }

        };

    private void setTemperature(final String targetTemp){
        if (myParticleDevice != null){


            Async.executeAsync(myParticleDevice, new Async.ApiWork<io.particle.android.sdk.cloud.ParticleDevice, Integer>() {

                @Override
                public Integer callApi(io.particle.android.sdk.cloud.ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                    Integer success = 0;


                    //the commands in functionCommandList will be executed by the Particle device
                    ArrayList functionCommandList = new ArrayList<String>();
                    functionCommandList.add(targetTemp);


                    try {
                        success = particleDevice.callFunction("setTempHoney",functionCommandList);

                    } catch (io.particle.android.sdk.cloud.ParticleDevice.FunctionDoesNotExistException e) {
                        Toaster.l(TempHoneywellActivity.this, "Function doesn't exist!");
                    }

                    return success;
                }


                public void onSuccess(Integer returnValue) {
                    if (returnValue == 1) {


                    }
                    else if (returnValue == -1){

                    }
                    else if (returnValue == -2){

                    }

                    //recyclerView.getAdapter().notifyDataSetChanged();
                }

                public void onFailure(ParticleCloudException e) {
                    Log.e("SOME_TAG", e.getBestMessage());
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
