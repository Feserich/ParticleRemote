package com.fese.particleremote;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class RelayActivity extends AppCompatActivity {

    private String myPhotonID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relay);

        myPhotonID = (String) getIntent().getSerializableExtra("deviceID");

        
    }
}
