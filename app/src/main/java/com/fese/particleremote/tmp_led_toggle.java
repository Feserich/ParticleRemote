package com.fese.particleremote;

import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toolbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Toaster;

public class tmp_led_toggle extends AppCompatActivity {

    private Button toggleLEDBtn;
    //private Toolbar toolbar;


    private io.particle.android.sdk.cloud.ParticleDevice myPhoton;
    private String myPhotonID = null;

    //test
    private List<String> paramlistON = new ArrayList<String>();
    private List<String> paramlistOFF = new ArrayList<String>();
    private boolean statusLED = false;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tmp_led_toggle);

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setActionBar(toolbar);

        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);


        myPhotonID = (String) getIntent().getSerializableExtra("deviceID");

        //get ID from GUI element
        toggleLEDBtn = (Button)findViewById(R.id.btn_toggleLED);

        //event handler
        toggleLEDBtn.setOnClickListener(toggleLEDOnClick);

        //test
        paramlistON.add("on");
        paramlistOFF.add("off");

        //get my Particle Device

        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, List<ParticleDevice>>() {

            public List<ParticleDevice> callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                return particleCloud.getDevices();
            }

            public void onSuccess(List<ParticleDevice> devices) {
                for (ParticleDevice device : devices) {
                    if (device.getID().equals(myPhotonID)) {
                        myPhoton = device;
                        Toaster.l(tmp_led_toggle.this, "Success! Found Device");
                        return;
                    }
                }
            }

            public void onFailure(ParticleCloudException e) {
                Log.e("SOME_TAG", e.getBestMessage());
                Toaster.l(tmp_led_toggle.this, "Cannot find Device: Wrong credentials or no internet connectivity, please try again");
            }
        });

    }


    View.OnClickListener toggleLEDOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            Async.executeAsync(myPhoton, new Async.ApiWork<ParticleDevice, Integer>() {

                public Integer callApi(ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                    Integer success = 0;
                    try{
                        if(statusLED==false){
                            success = myPhoton.callFunction("tglLED", paramlistON);
                            statusLED = true;

                        }else{
                            success = myPhoton.callFunction("tglLED", paramlistOFF);
                            statusLED = false;

                        }

                    }catch (ParticleDevice.FunctionDoesNotExistException e) {
                        Toaster.s(tmp_led_toggle.this, "Function does not exist");
                        //TODO: Wenn der Photon offline ist, wird "exception" und "onSuccess" gleichzeitig aufgerufen
                    }


                    return success;
                }

                public void onSuccess(Integer returnValue) {
                    Toaster.s(tmp_led_toggle.this, "LED on D7 toggeled successfully");
                }

                public void onFailure(ParticleCloudException e) {
                    Log.e("SOME_TAG", e.getBestMessage());
                }


            });

        }



    };

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
