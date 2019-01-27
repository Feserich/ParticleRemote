package com.fese.particleremote;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
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
import android.widget.DatePicker;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.TimePicker;
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

import static java.lang.Math.toIntExact;


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
        Button btn_setFutureTemp = findViewById(R.id.btn_setFutureTemp);

        setTempToast = new LoadToast(this);
        setTempToast.setText(getString(R.string.setting_temperature_toast_text));
        //setTempToast.setTranslationY(200);                //translation in pixels

        btn_setTemp.setOnClickListener(setTempOnClick);
        btn_setTempOff.setOnClickListener(setTempOffOnClick);
        btn_setTempComfort.setOnClickListener(setTempComfortOnClick);
        btn_setTempNight.setOnClickListener(setTempNightOnClick);
        btn_setFutureTemp.setOnClickListener(openFutureTempTimePickerOnClick);




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

    private void setFutureTemperature(String targetFutureTemp, long diffMiliseconds){

        int minutesTillHeatingStart;
        String delimeter = ";";

        minutesTillHeatingStart = (int)Math.ceil((double)diffMiliseconds / 60000);

        final String command = targetFutureTemp + delimeter
                + String.valueOf(minutesTillHeatingStart) + delimeter;

        if (myParticleDevice != null){

            setTempToast.show();
            Async.executeAsync(myParticleDevice, new Async.ApiWork<io.particle.android.sdk.cloud.ParticleDevice, Integer>() {

                @Override
                public Integer callApi(io.particle.android.sdk.cloud.ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                    Integer success = 0;

                    //the commands in functionCommandList will be executed by the Particle device
                    ArrayList<String> functionCommandList = new ArrayList<>();
                    functionCommandList.add(command);

                    try {
                        success = particleDevice.callFunction("setTempFut",functionCommandList);

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

    View.OnClickListener openFutureTempTimePickerOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            Calendar mcurrentDate = Calendar.getInstance();
            final int mYear = mcurrentDate.get(Calendar.YEAR);
            final int mMonth = mcurrentDate.get(Calendar.MONTH);
            final int mDay = mcurrentDate.get(Calendar.DAY_OF_MONTH);
            final int hour = mcurrentDate.get(Calendar.HOUR_OF_DAY);
            final int minute = mcurrentDate.get(Calendar.MINUTE);

            final long currentUnixTimestamp = mcurrentDate.getTimeInMillis();

            DatePickerDialog mDatePicker;
            mDatePicker = new DatePickerDialog(TempHoneywellActivity.this, new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker datepicker, final int selectedyear, final int selectedmonth, final int selectedday) {

                    TimePickerDialog mTimePicker;
                    mTimePicker = new TimePickerDialog(TempHoneywellActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                            String targetTempStr;

                            if (targetTemp.equals(getString(R.string.LOWEST_TEMPERATURE_SELECTION_VALUE))) {
                                targetTempStr = getString(R.string.HONEYWELL_OFF_VALUE);    //value to set Honeywell to OFF
                            }
                            else {
                                targetTempStr = targetTemp + "0";                           //multiple 10 for RAW value == add a zero

                            }
                            Calendar calenderInstance = new GregorianCalendar(selectedyear,
                                    selectedmonth, selectedday, selectedHour, selectedMinute);
                            long selectedUnixTimeStamp = calenderInstance.getTimeInMillis();
                            setFutureTemperature(targetTempStr, selectedUnixTimeStamp - currentUnixTimestamp);
                        }
                    }, hour, minute, true);//Yes 24 hour time
                    mTimePicker.setTitle("Select Heat Start Time");
                    mTimePicker.show();

                }
            }, mYear, mMonth, mDay);
            mDatePicker.setTitle("Select Heat Start Date");
            mDatePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            mDatePicker.getDatePicker().setMaxDate(System.currentTimeMillis() + 7 * 24 * 3600 * 1000);
            mDatePicker.show();

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
