package com.zavijavasoft.jaacad;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class NoInternetActivity extends AppCompatActivity {

    private static final int CHECK_INTERNET = 1;

    // this handler will receive a delayed message
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        Button btnCheckInternet = findViewById(R.id.buttonCheckInternet);
        btnCheckInternet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncCheckInternetConnection async = new AsyncCheckInternetConnection();
                async.execute(NoInternetActivity.this);
            }
        });

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // Do task here
                if (msg.what == CHECK_INTERNET) {
                    AsyncCheckInternetConnection async = new AsyncCheckInternetConnection();
                    async.execute(NoInternetActivity.this);
                    sendEmptyMessageDelayed(CHECK_INTERNET, 5000);
                }
            }
        };

    }

    @Override
    protected void onResume() {
        handler.sendEmptyMessageDelayed(CHECK_INTERNET, 5000);
        super.onResume();
    }

    @Override
    protected void onPause() {
        handler.removeMessages(CHECK_INTERNET);
        super.onPause();
    }
}
