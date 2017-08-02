package com.wtuadn.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.wtuadn.thirdpartutils.ThirdPartUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ThirdPartUtils.init(this);
    }
}
