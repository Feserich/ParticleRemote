package com.fese.particleremote;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        initView();
    }

    private void initView() {
        LinearLayout ll_issue_about = (LinearLayout) findViewById(R.id.ll_issue_about);
        LinearLayout ll_github_about = (LinearLayout) findViewById(R.id.ll_github_about);
        LinearLayout ll_credits_about = (LinearLayout) findViewById(R.id.ll_credits_about);
        ScrollView scroll_about = (ScrollView) findViewById(R.id.scroll_about);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.anim_about_card_show);
        scroll_about.startAnimation(animation);

        ll_issue_about.setOnClickListener(this);
        ll_github_about.setOnClickListener(this);
        ll_credits_about.setOnClickListener(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_about_share);
        fab.setOnClickListener(this);

        TextView tv_about_version_number = (TextView) findViewById(R.id.tv_about_version_number);
        tv_about_version_number.setText(getVersionName());
    }


    @Override
    public void onClick(View view) {

        Intent intent = new Intent();

        switch (view.getId()) {
            case R.id.ll_issue_about:
                intent.setData(Uri.parse(Constant.GITHUB_ISSUE));
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);
                break;

            case R.id.ll_github_about:
                intent.setData(Uri.parse(Constant.GITHUB_APP));
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);
                break;

            case R.id.ll_credits_about:
                intent = new Intent(AboutActivity.this, CreditsActivity.class);
                startActivity(intent);
                break;

            case R.id.fab_about_share:
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, Constant.SHARE_CONTENT);
                intent.setType("text/plain");
                startActivity(Intent.createChooser(intent, "Share"));
                break;
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public String getVersionName() {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            String version = info.versionName;
            return getString(R.string.about_version) + " " + version;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
