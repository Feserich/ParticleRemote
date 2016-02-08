package com.fese.particleremote;

import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toolbar;

import java.io.IOException;
import java.util.List;
import java.util.logging.Handler;

import io.particle.android.sdk.cloud.*;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class TempHumiActivity extends AppCompatActivity {
    private TextView temperatureTV;
    private TextView humidityTV;
    private String myPhotonID = null;
    private io.particle.android.sdk.cloud.ParticleDevice myPhoton;
    private String CloudVariableTemp = "temperature";
    private String CloudVariableHumi = "humidity";
    double temperatureVal;
    double humidityVal;
    private boolean loopMeasurement = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_humi);

        temperatureTV = (TextView)findViewById(R.id.tv_temperature);
        humidityTV = (TextView)findViewById(R.id.tv_humidity);

        myPhotonID = (String) getIntent().getSerializableExtra("deviceID");

        //TODO: Detect if activity has been stopped or paused then set loopMeasurement = false
        loopMeasurement = true;

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setActionBar(toolbar);

        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        getTemperatureAndHumidity();
    }



    private void getTemperatureAndHumidity(){
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Boolean>() {

            @Override
            public Boolean callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                boolean success = false;

                particleCloud.getDevices();
                myPhoton = particleCloud.getDevice(myPhotonID);

                try {
                    temperatureVal = myPhoton.getDoubleVariable(CloudVariableTemp);
                    humidityVal = myPhoton.getDoubleVariable(CloudVariableHumi);
                    success = true;
                }

                catch (ParticleDevice.VariableDoesNotExistException e){
                    Toaster.l(TempHumiActivity.this, "Error reading variable!");
                }

                return success;
            }

            public void onSuccess(Boolean onSuccess) {
                if (onSuccess && loopMeasurement) {
                    final android.os.Handler refreshValHandler = new android.os.Handler();
                    refreshValHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getTemperatureAndHumidity();
                        }
                    }, 3000);
                }

                setValuesToEditText();
            }

            public void onFailure(ParticleCloudException e) {
                Log.e("SOME_TAG", e.getBestMessage());

            }


        });

    }


    public void setValuesToEditText(){
        temperatureVal = (double)Math.round(temperatureVal * 100)/ 100d;
        humidityVal = (double)Math.round(humidityVal * 100)/ 100d;

        temperatureTV.setText("Temperature: " + String.valueOf(temperatureVal) + "°C");
        humidityTV.setText("Humidity: " + String.valueOf(humidityVal) + "%");
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.home:
                loopMeasurement = false;
                NavUtils.navigateUpFromSameTask(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }
}
