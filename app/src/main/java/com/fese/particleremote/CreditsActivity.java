package com.fese.particleremote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CreditsActivity extends AppCompatActivity {

    private List<Credit> creditList = new ArrayList<>();
    private RecyclerView recyclerView;
    private RVadapterCredits mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        recyclerView = (RecyclerView) findViewById(R.id.RV_creditsList);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        mAdapter = new RVadapterCredits(creditList);
        recyclerView.setAdapter(mAdapter);

        initalizeCreditValues();




        RVadapterCredits.CreditViewHolder.setOnCreditClickedListener(new RVadapterCredits.OnCreditClickedListener() {
            @Override
            public void onCreditClicked(String creditLink) {
                Intent intent = new Intent();
                intent.setData(Uri.parse(creditLink));
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);

            }
        });



    }

    private void initalizeCreditValues() {

        Credit credit = new Credit("Particle Android Cloud SDK", "Particle Industries, Inc.", "https://github.com/spark/spark-sdk-android", "Apache License, Version 2.0");
        creditList.add(credit);

        credit = new Credit("Particle Device Setup library", "Particle Industries, Inc.", "https://github.com/spark/spark-setup-android", "Apache License, Version 2.0");
        creditList.add(credit);

        credit = new Credit("Butter Knife", "Jake Wharton", "https://github.com/JakeWharton/butterknife", "Apache License, Version 2.0");
        creditList.add(credit);

        credit = new Credit("Discrete Seekbar", "Gustavo Claramunt", "https://github.com/AnderWeb/discreteSeekBar", "Apache License, Version 2.0");
        creditList.add(credit);

        credit = new Credit("Load Toast Library", "code-mc", "https://github.com/code-mc/loadtoast", "Apache License, Version 2.0");
        creditList.add(credit);

        credit = new Credit("MPAndroidChart", "Philipp Jahoda", "https://github.com/PhilJay/MPAndroidChart", "Apache License, Version 2.0");
        creditList.add(credit);

        credit = new Credit("Material Dialogs", "Aidan Michael Follestad", "https://github.com/afollestad/material-dialogs", "MIT License");
        creditList.add(credit);

        credit = new Credit("Android Sources", "The Android Open Source Project", "https://source.android.com/", "Apache License, Version 2.0");
        creditList.add(credit);



        mAdapter.notifyDataSetChanged();
    }

    //TODO: open source link on click
    //startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.stackoverflow.com")));
}
