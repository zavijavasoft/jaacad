package com.zavijavasoft.jaacad;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class NoInternetActivity extends AppCompatActivity {

    private static final int CHECK_INTERNET = 1;

    private ResultReceiver resultReceiver;
    // this handler will receive a delayed message
    private Handler handler;
    private boolean firstShow = true;

    NoInternetActivity(){
        super();
        resultReceiver = new ResultReceiver(new Handler()){
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                switch (resultCode) {
                    case CoreService.INTERNET_OK:
                        Intent intent = new Intent(getApplicationContext(), GalleryActivity.class);
                        intent.putExtra(GalleryActivity.KEY_INTERNET_APPROVED, true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        break;
                    case CoreService.NETWORK_EXCEPTION:
                        String message = resultData.getString(CoreService.KEY_RESULT_NETWORK_EXCEPTION);
                        Toast toast = Toast.makeText(getApplicationContext(),
                                message, Toast.LENGTH_SHORT);
                        toast.show();
                    case CoreService.INTERNET_LOST:
                        break;
                }
                super.onReceiveResult(resultCode, resultData);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);
        firstShow = true;

        Button btnCheckInternet = findViewById(R.id.buttonCheckInternet);
        btnCheckInternet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent intent = new Intent(NoInternetActivity.this, CoreService.class);
               intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.CHECK_INTERNET_CONNECTION);
               intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
               startService(intent);
            }
        });


        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // Do task here
                if (msg.what == CHECK_INTERNET) {
                    Intent intent = new Intent(NoInternetActivity.this, CoreService.class);
                    intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.CHECK_INTERNET_CONNECTION);
                    intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
                    startService(intent);
                    sendEmptyMessageDelayed(CHECK_INTERNET, 5000);
                }
            }
        };

    }

    @Override
    protected void onResume() {
        handler.sendEmptyMessageDelayed(CHECK_INTERNET, 5000);
        if (firstShow) {
            Intent intent = getIntent();
            String message = intent.getStringExtra(CoreService.KEY_RESULT_NETWORK_EXCEPTION);
            if (message != null) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        message, Toast.LENGTH_SHORT);
                toast.show();
            }
            firstShow = false;
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        handler.removeMessages(CHECK_INTERNET);
        super.onPause();
    }
}
