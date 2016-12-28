package com.fese.particleremote;

import android.graphics.Color;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;

import io.particle.android.sdk.cloud.*;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class TempHumiActivity extends AppCompatActivity {
    private TextView temperatureTV;
    private TextView humidityTV;
    private String myParticleID = null;
    private io.particle.android.sdk.cloud.ParticleDevice myParticleDevice;
    private String CloudVariableTemp = "temperature";
    private String CloudVariableHumi = "humidity";
    double temperatureVal;
    double humidityVal;
    private boolean loopMeasurement = false;
    private LineChart tempHumiChart;
    private static final int REFRESH_CYCLE = 3000;          //unit: [ms]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_humi);

        temperatureTV = (TextView)findViewById(R.id.tv_temperature);
        humidityTV = (TextView)findViewById(R.id.tv_humidity);

        tempHumiChart = (LineChart) findViewById(R.id.tempHumiChart);
        LineData data = new LineData();
        tempHumiChart.setData(data);

        Legend l = tempHumiChart.getLegend();


        //TODO: Detect if activity has been stopped or paused then set loopMeasurement = false
        loopMeasurement = true;

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //call Methods
        getParticleDeviceInstance();
        getTemperatureAndHumidity();

        //TODO: Runnable to Refresh the Data automaticly
        final android.os.Handler refreshValHandler = new android.os.Handler();
        refreshValHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getTemperatureAndHumidity();
            }
        }, REFRESH_CYCLE);
    }



    private void getTemperatureAndHumidity(){
        if (myParticleDevice != null) {
            Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Boolean>() {

                @Override
                public Boolean callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                    boolean success = false;

                    try {
                        temperatureVal = myParticleDevice.getDoubleVariable(CloudVariableTemp);
                        humidityVal = myParticleDevice.getDoubleVariable(CloudVariableHumi);
                        success = true;
                    }

                    catch (ParticleDevice.VariableDoesNotExistException e){
                        Toaster.l(TempHumiActivity.this, "Error reading variable!");
                    }

                    return success;
                }

                public void onSuccess(Boolean onSuccess) {
                    addChartEntry(temperatureVal);
                    if (onSuccess && loopMeasurement) {
                        final android.os.Handler refreshValHandler = new android.os.Handler();
                        refreshValHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getTemperatureAndHumidity();
                            }
                        }, REFRESH_CYCLE);
                    }

                    setValuesToEditText();
                }

                public void onFailure(ParticleCloudException e) {
                    Log.e("SOME_TAG", e.getBestMessage());

                }


            });

        }


    }


    public void setValuesToEditText(){
        temperatureVal = (double)Math.round(temperatureVal * 100)/ 100d;
        humidityVal = (double)Math.round(humidityVal * 100)/ 100d;

        temperatureTV.setText("Temperature: " + String.valueOf(temperatureVal) + "°C");
        humidityTV.setText("Humidity: " + String.valueOf(humidityVal) + "%");
    }


    public void addChartEntry(double tempValueX){
        LineData data = tempHumiChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount() * (REFRESH_CYCLE/1000), (float) tempValueX), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            tempHumiChart.notifyDataSetChanged();

            // limit the number of visible entries
            tempHumiChart.setVisibleXRangeMaximum(120);
            // tempHumiChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            tempHumiChart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }

    }

    private LineDataSet createSet(){
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
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
                Toaster.l(TempHumiActivity.this, e.getBestMessage());
            }

        });

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
