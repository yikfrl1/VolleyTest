package com.hannto.volleytest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.hannto.io.HanntoError;
import com.hannto.io.HanntoNetworkResponse;
import com.hannto.io.HanntoRequest;
import com.hannto.io.HanntoRequestQueue;
import com.hannto.io.HanntoResponse;
import com.hannto.io.toolbox.HanntoBaseRequest;
import com.hannto.io.toolbox.HanntoVolley;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final byte[] FIRST_REQUEST = new byte[]{0x01};
    public static final byte[] SECOND_REQUEST = new byte[]{0x02};
    public static final byte[] THIRD_REQUEST = new byte[]{0x03};

    public static final byte[] FIRST_RESPONSE = new byte[]{0x11};
    public static final byte[] SECOND_RESPONSE = new byte[]{0x12};
    public static final byte[] THIRD_RESPONSE = new byte[]{0x13};
    public static final byte[] OTHER_RESPONSE = new byte[]{0x14};
    HanntoRequestQueue hanntoRequestQueue;


    private Button button_first, button_second, button_third;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLog();
        button_first = findViewById(R.id.button_first);
        button_first.setOnClickListener(this);

        button_second = findViewById(R.id.button_second);
        button_second.setOnClickListener(this);

        button_third = findViewById(R.id.button_third);
        button_third.setOnClickListener(this);

        hanntoRequestQueue = HanntoVolley.newRequestQueue(MainActivity.this);
    }

    private void initLog() {
        Logger.clearLogAdapters();
        PrettyFormatStrategy prettyFormatStrategy = PrettyFormatStrategy.newBuilder()
                .tag("VolleyTest")
                .build();
        Logger.addLogAdapter(new AndroidLogAdapter(prettyFormatStrategy));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_first:
                start(FIRST_REQUEST);
                break;
            case R.id.button_second:
                start(SECOND_REQUEST);
                break;
            case R.id.button_third:
                start(THIRD_REQUEST);
                break;
            default:
                break;
        }
    }

    private void start(byte[] data) {
        hanntoRequestQueue.add(new HanntoBaseRequest(data, new HanntoResponse.Listener<byte[]>() {
            @Override
            public void onResponse(byte[] response) {
                Logger.w(String.valueOf(response[0]));
            }
        }, new HanntoResponse.ErrorListener() {
            @Override
            public void onErrorResponse(HanntoError error) {
                Logger.e(error.getMessage());
            }
        }));
    }
}
