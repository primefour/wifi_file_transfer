package com.example.TransmitWifi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


/**
 *Just for the test activity
 *@author haihui.li
 *@version 1.0.0
 *
 */

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    /**
     * Called when the activity is first created.
     * @param savedInstanceState state backup
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_main);

        Button apButton = (Button)findViewById(R.id.ap_button);
        Button cliButton = (Button)findViewById(R.id.client_button);
        apButton.setOnClickListener(mAPButtonEvent);
        cliButton.setOnClickListener(mCliButtonEvent);
    }

    /*
     *start a activity for build access pointer
     */
    private View.OnClickListener mAPButtonEvent = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, APWifiActivity.class);
            startActivity(intent);
        }
    };

    /*
     *start client activity
     */
    private View.OnClickListener mCliButtonEvent = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, WifiClientActivity.class);
            startActivity(intent);
        }
    };
}
