package com.fese.particleremote;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import io.particle.android.sdk.cloud.*;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;


public class TempHumiActivity extends AppCompatActivity {
    private TextView temperatureTV;
    private TextView humidityTV;
    private String myParticleID = null;
    private io.particle.android.sdk.cloud.ParticleDevice myParticleDevice;
    private String CloudVariableTemp = "temperature";
    private String CloudVariableHumi = "humidity";
    double temperatureVal;
    double humidityVal;
    private LineChart tempHumiChart;
    private ArrayList<ILineDataSet> dataSets;
    final android.os.Handler handler = new android.os.Handler();
    private View viewTempHumi;


    private int refresh_cycle;                                          //unit: [ms]
    private static final float MIN_TEMPERATURE_DEFAULT_VALUE = 10;
    private static final float MAX_TEMPERATURE_DEFAULT_VALUE = 30;
    private static final float MIN_HUMIDITY_DEFAULT_VALUE = 0;
    private static final float MAX_HUMIDITY_DEFAULT_VALUE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_humi);

        temperatureTV = (TextView)findViewById(R.id.tv_temperature);
        humidityTV = (TextView)findViewById(R.id.tv_humidity);
        viewTempHumi = (RelativeLayout) findViewById(R.id.RelLay_tempHumi);


        //Init Line Chart
        tempHumiChart = (LineChart) findViewById(R.id.tempHumiChart);
        dataSets = new ArrayList<ILineDataSet>();
        LineData data = new LineData(dataSets);
        tempHumiChart.setData(data);
        tempHumiChart.getDescription().setEnabled(false);

        //Format the humidity yAxis
        YAxis yAxisRight = tempHumiChart.getAxisRight();
        yAxisRight.setValueFormatter(new MyYAxisHumidityFormatter());
        yAxisRight.setAxisMinimum(MIN_HUMIDITY_DEFAULT_VALUE);
        yAxisRight.setAxisMaximum(MAX_HUMIDITY_DEFAULT_VALUE);
        yAxisRight.setLabelCount(11, true);

        //Format the temperature yAxis
        YAxis yAxisLeft = tempHumiChart.getAxisLeft();
        yAxisLeft.setLabelCount(11, true);
        yAxisLeft.setAxisMinimum(MIN_TEMPERATURE_DEFAULT_VALUE);
        yAxisLeft.setAxisMaximum(MAX_TEMPERATURE_DEFAULT_VALUE);
        yAxisLeft.setValueFormatter(new MyYAxisTemperatureFormatter());

        //set toolbar and navigate up arrow
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //call Methods
        ParticleCloudSDK.init(this);
        getParticleDeviceInstance();
        getTemperatureAndHumidity();

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
                        //Log.e("SOME_TAG", e.getMessage().toString());
                        Snackbar snackbarError = Snackbar
                                .make(viewTempHumi, e.getMessage().toString(), Snackbar.LENGTH_LONG);
                        snackbarError.show();
                    }

                    return success;
                }

                public void onSuccess(Boolean onSuccess) {
                    if (onSuccess){
                        addTempHumiValueToChart(temperatureVal, humidityVal);
                        setValuesToEditText();
                    }

                }

                public void onFailure(ParticleCloudException e) {
                    Log.e("SOME_TAG", e.getBestMessage());
                    Snackbar snackbarError = Snackbar
                            .make(viewTempHumi, e.getBestMessage(), Snackbar.LENGTH_LONG);
                    snackbarError.show();

                }


            });

        }


    }

    private void setValuesToEditText(){
        temperatureVal = (double)Math.round(temperatureVal * 100)/ 100d;
        humidityVal = (double)Math.round(humidityVal * 100)/ 100d;

        temperatureTV.setText(getString(R.string.temperature)+ ": " + String.valueOf(temperatureVal) + " °C");
        humidityTV.setText(getString(R.string.humidity) + ": " + String.valueOf(humidityVal) + " %");
    }

    private void addTempHumiValueToChart(double temperatureValueX, double humidityValueX){

        LineData data = tempHumiChart.getData();

        if (data != null) {
            ILineDataSet tempSet = data.getDataSetByIndex(0);

            if (tempSet == null) {
                tempSet = createTemperatureSet();
                dataSets.add(tempSet);

            }

            ILineDataSet humiSet = data.getDataSetByIndex(1);

            if (humiSet == null) {
                humiSet = createHumiditySet();
                dataSets.add(humiSet);
            }

            if (temperatureValueX < MIN_TEMPERATURE_DEFAULT_VALUE){
                YAxis yAxisLeft = tempHumiChart.getAxisLeft();
                yAxisLeft.resetAxisMinimum();
            }
            else if (temperatureValueX > MAX_TEMPERATURE_DEFAULT_VALUE){
                YAxis yAxisLeft = tempHumiChart.getAxisLeft();
                yAxisLeft.resetAxisMaximum();
            }

            data.addEntry(new Entry(tempSet.getEntryCount() * (refresh_cycle/1000), (float) temperatureValueX), 0);
            data.addEntry(new Entry(humiSet.getEntryCount() * (refresh_cycle/1000), (float) humidityValueX), 1);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            tempHumiChart.notifyDataSetChanged();

            // limit the number of visible entries
            tempHumiChart.setVisibleXRangeMaximum(3600);
            // tempHumiChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            tempHumiChart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);

        }

    }

    private void getRefreshCycleFromSettings(){
        Log.i("SOME_TAG", "getRefreshCycleFromSettings");
        SharedPreferences SharedPref  = PreferenceManager.getDefaultSharedPreferences(this);

        String refreshCycleString = SharedPref.getString(getString(R.string.pref_refresh_cycle_chart_key), getString(R.string.refresh_cycle_chart_default_value));
        refresh_cycle = Integer.valueOf(refreshCycleString) * 1000;

    }

    private LineDataSet createTemperatureSet(){
        LineDataSet set = new LineDataSet(null, getString(R.string.temperature) + " [°C]");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.MATERIAL_COLORS[1]);
        //set.setCircleColor(Color.WHITE);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setLineWidth(2f);
        //set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private LineDataSet createHumiditySet(){
        LineDataSet set = new LineDataSet(null, getString(R.string.humidity) + " [%]");
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set.setColor(ColorTemplate.MATERIAL_COLORS[3]);
        //set.setCircleColor(Color.WHITE);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setLineWidth(2f);
        //set.setCircleRadius(4f);
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
                Snackbar snackbarError = Snackbar
                        .make(viewTempHumi, e.getBestMessage(), Snackbar.LENGTH_LONG);
                snackbarError.show();
            }

        });

    }


    //TODO: start a service in the background for recording the data + a notification message (battery drain!)
    private Runnable getNewValuesRunnable = new Runnable() {
        @Override
        public void run() {

            getTemperatureAndHumidity();
            handler.postDelayed(getNewValuesRunnable, refresh_cycle);

        }
    };

    @Override
    protected void onPause() {
        handler.removeCallbacks(getNewValuesRunnable);
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        getRefreshCycleFromSettings();
        handler.post(getNewValuesRunnable);
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_temp_humi_activity, menu);
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
                Intent intentSettings = new Intent(TempHumiActivity.this, UserSettingsActivity.class);
                TempHumiActivity.this.startActivity(intentSettings);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }


}

class MyYAxisTemperatureFormatter implements IAxisValueFormatter {

    private DecimalFormat mFormat;

    public MyYAxisTemperatureFormatter () {
        mFormat = new DecimalFormat("###,###,##0.0"); // use one decimal
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        // write your logic here
        // access the YAxis object to get more information
        return mFormat.format(value) + " °C"; // e.g. append a dollar-sign
    }
}

class MyYAxisHumidityFormatter implements IAxisValueFormatter {

    private DecimalFormat mFormat;

    public MyYAxisHumidityFormatter () {
        mFormat = new DecimalFormat("###,###,##0"); // use no decimal
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        // write your logic here
        // access the YAxis object to get more information
        return mFormat.format(value) + " %"; // e.g. append a dollar-sign
    }
}
