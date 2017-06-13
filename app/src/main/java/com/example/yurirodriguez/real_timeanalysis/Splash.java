package com.example.yurirodriguez.real_timeanalysis;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Typeface mytapeface = Typeface.createFromAsset(getAssets(),"champagne.ttf");
        TextView mytextview = (TextView)findViewById(R.id.textView2);
        mytextview.setTypeface(mytapeface);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Splash.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        },2000);
    }
}
