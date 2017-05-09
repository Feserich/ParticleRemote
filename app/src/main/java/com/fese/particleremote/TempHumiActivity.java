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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.particle.android.sdk.cloud.*;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;


public class TempHumiActivity extends AppCompatActivity {
    private TextView temperatureTV;
    private TextView humidityTV;
    private String myParticleID = null;
    private io.particle.android.sdk.cloud.ParticleDevice myParticleDevice;
    double temperatureVal;
    double humidityVal;
    String temperatureValuesChain = "";
    String humidityValuesChain = "";
    private LineChart tempHumiChart;
    private ArrayList<ILineDataSet> dataSets;
    final android.os.Handler handler = new android.os.Handler();
    private View viewTempHumi;
    //private Long referenceTimestamp = ((System.currentTimeMillis()/1000) - (25 * 3600));


    //static
    private int refresh_cycle;                                          //unit: [ms]
    private static final float MIN_TEMPERATURE_DEFAULT_VALUE = 10;
    private static final float MAX_TEMPERATURE_DEFAULT_VALUE = 30;
    private static final float MIN_HUMIDITY_DEFAULT_VALUE = 0;
    private static final float MAX_HUMIDITY_DEFAULT_VALUE = 100;
    private static final String CloudVariableTempLabel = "temperature";
    private static final String CloudVariableHumiLabel = "humidity";
    private static final String CloudVariableTempHistoryLabel = "tempRec";
    private static final String CloudVariableHumiHistoryLabel = "humiRec";


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


        //TODO: show a MarkerView, class is prepared in the bottom code, xml layout can be found in the github link (also at the bottom)

        //MyMarkerView myMarkerView= new MyMarkerView(getApplicationContext(), R.layout.my_marker_view_layout, referenceTimestamp);
        //mChart.setMarkerView(myMarkerView);

        //Format the time xAxis
        XAxis xAxis = tempHumiChart.getXAxis();
        xAxis.setValueFormatter(new MyXAxisTimeFormatter());
        //xAxis.setAxisMinimum(0);
        //xAxis.setAxisMaximum(25 * 3600 * 1000);
        xAxis.setLabelCount(6, true);

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
                        temperatureVal = myParticleDevice.getDoubleVariable(CloudVariableTempLabel);
                        humidityVal = myParticleDevice.getDoubleVariable(CloudVariableHumiLabel);
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
                        //addTempHumiValueToChart((float) temperatureVal, (float) humidityVal, refresh_cycle/1000);
                        if (temperatureVal == -100 || humidityVal == -100)
                        {
                            Snackbar snackbarError = Snackbar
                                    .make(viewTempHumi, R.string.DHT22_error_snackbar_message, Snackbar.LENGTH_LONG);
                            snackbarError.show();
                        }
                        else
                        {
                            setValuesToEditText();
                        }

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
        else {
            handler.postDelayed(new Runnable() {
                public void run() {
                    getTemperatureAndHumidity();
                }
            }, 1000);
        }
    }





    //TODO: add menu "fast record" => show in the diagram the "live" values


    private void getTemperatureAndHumidityHistory(){


        if (myParticleDevice != null) {
            Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Boolean>() {

                @Override
                public Boolean callApi(ParticleCloud particleCloud) throws ParticleCloudException, IOException {
                    boolean success = false;

                    try {
                        temperatureValuesChain = myParticleDevice.getStringVariable(CloudVariableTempHistoryLabel);
                        humidityValuesChain = myParticleDevice.getStringVariable(CloudVariableHumiHistoryLabel);
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

                        String[] temperatureValuesArray = temperatureValuesChain.split(";");
                        String[] humidityValuesArray = humidityValuesChain.split(";");

                        //TODO: get a timestamp of the last value, e.g.: array[0] = lastTimestamp (the newest value could be 0-59min old)
                        //displaying the oldest value in the array first
                        for(int i = (temperatureValuesArray.length - 1); i >= 0; i--)
                        {

                            try {
                                addTempHumiValueToChart(Float.parseFloat(temperatureValuesArray[i])/10, Float.parseFloat(humidityValuesArray[i]), (System.currentTimeMillis()/1000 - (i * 3600)));
                            }
                            catch(NumberFormatException ex) {
                                Log.e("SOME_TAG", ex.getMessage());
                                Snackbar snackbarNumberCheck = Snackbar
                                        .make(viewTempHumi, R.string.empty_value_temperature_history_snackbar_message, Snackbar.LENGTH_LONG);
                                snackbarNumberCheck.show();
                            }

                        }


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
        else {
            handler.postDelayed(new Runnable() {
                public void run() {
                    getTemperatureAndHumidityHistory();
                }
            }, 1000);
        }
    }

    private void clearDiagram(){
        LineData data = tempHumiChart.getData();
        data.clearValues();
    }

    private void setValuesToEditText(){
        temperatureVal = (double)Math.round(temperatureVal * 100)/ 100d;
        humidityVal = (double)Math.round(humidityVal * 100)/ 100d;

        temperatureTV.setText(getString(R.string.temperature)+ ": " + String.valueOf(temperatureVal) + " °C");
        humidityTV.setText(getString(R.string.humidity) + ": " + String.valueOf(humidityVal) + " %");
    }

    private void addTempHumiValueToChart(float temperatureValueY, float humidityValueY, float timeValueX){

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

            if (temperatureValueY < MIN_TEMPERATURE_DEFAULT_VALUE){
                YAxis yAxisLeft = tempHumiChart.getAxisLeft();
                yAxisLeft.resetAxisMinimum();
            }
            else if (temperatureValueY > MAX_TEMPERATURE_DEFAULT_VALUE){
                YAxis yAxisLeft = tempHumiChart.getAxisLeft();
                yAxisLeft.resetAxisMaximum();
            }

            data.addEntry(new Entry(timeValueX, temperatureValueY), 0);
            data.addEntry(new Entry(timeValueX, humidityValueY), 1);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            tempHumiChart.notifyDataSetChanged();

            // limit the number of visible entries
            tempHumiChart.setVisibleXRangeMaximum(48*3600);
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
        clearDiagram();
        getTemperatureAndHumidityHistory();
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

            case R.id.menu_history:
                clearDiagram();
                getTemperatureAndHumidityHistory();
                return true;

            case R.id.menu_clear_diagram:
                clearDiagram();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }


}

class MyXAxisTimeFormatter implements IAxisValueFormatter {

    //private long referenceTimestamp; // minimum timestamp in your data set
    private DateFormat mDataFormat;
    private Date mDate;

    public MyXAxisTimeFormatter() {
        //this.referenceTimestamp = referenceTimestamp;
        this.mDataFormat = new SimpleDateFormat("EEE, HH:mm", Locale.ENGLISH);
        this.mDate = new Date();
    }



    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        // convertedTimestamp = originalTimestamp - referenceTimestamp
        long convertedTimestamp = (long) value;

        // Retrieve original timestamp
        //long originalTimestamp = referenceTimestamp + convertedTimestamp;

        // Convert timestamp to hour:minute
        return getHour(convertedTimestamp);
    }


    public int getDecimalDigits() {
        return 0;
    }

    private String getHour(long timestamp){
        try{
            mDate.setTime(timestamp*1000);
            return mDataFormat.format(mDate);
        }
        catch(Exception ex){
            return "xx";
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

/**
 * The following class for the MarkerView must be edited
 * The conversion of the referenceTimestamp must be deleted for my solution
 * See MyXAxisTimeFormatter
 *
 * https://github.com/PhilJay/MPAndroidChart/issues/789#issuecomment-241507904
 *
 */


/*
public class MyMarkerView extends MarkerView {

    private TextView tvContent;
    private long referenceTimestamp;  // minimum timestamp in your data set
    private DateFormat mDataFormat;
    private Date mDate;

    public MyMarkerView (Context context, int layoutResource, long referenceTimestamp) {
        super(context, layoutResource);
        // this markerview only displays a textview
        tvContent = (TextView) findViewById(R.id.tvContent);
        this.referenceTimestamp = referenceTimestamp;
        this.mDataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        this.mDate = new Date();
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        long currentTimestamp = (int)e.getX() + referenceTimestamp;

        tvContent.setText(e.getY() + "% at " + getTimedate(currentTimestamp)); // set the entry-value as the display text
    }

    @Override
    public int getXOffset(float xpos) {
        // this will center the marker-view horizontally
        return -(getWidth() / 2);
    }

    @Override
    public int getYOffset(float ypos) {
        // this will cause the marker-view to be above the selected value
        return -getHeight();
    }

    private String getTimedate(long timestamp){

        try{
            mDate.setTime(timestamp*1000);
            return mDataFormat.format(mDate);
        }
        catch(Exception ex){
            return "xx";
        }
    }
}
*/