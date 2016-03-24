package com.phairy.taxionly;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class Start extends Activity {

    int firstExecute ;
    static String TAG = "TaxiOnly";
    static String URL = "http://192.168.0.15:8080/";
    static String SURL = "https://192.168.0.15:8080/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Log.e(Start.TAG, "--Start--");

        final Intent myIntent;

        firstExecute = 1;
        //로그인 값 저장되어있을 경우//
        if (firstExecute == 1) {
       //     Log.e("hairyd", "Start->");
            myIntent = new Intent(getApplicationContext(), MainMenu.class);
        } else {
            Log.d(Start.TAG, "Start->MainMenu");
            myIntent = new Intent(getApplicationContext(), MainMenu.class);
        }
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                startActivity(myIntent);
                finish();
            }
        };

        handler.sendEmptyMessageDelayed(0, 2000);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}