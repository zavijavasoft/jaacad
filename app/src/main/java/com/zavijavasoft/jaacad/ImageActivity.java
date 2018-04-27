package com.zavijavasoft.jaacad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.yandex.disk.rest.ProgressListener;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;

import java.io.File;
import java.io.IOException;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ImageActivity extends AppCompatActivity {

    public static final String KEY_IMAGE_URL = "IMAGE_URL";
    public static final String KEY_THUMBNAIL_FILENAME = "THUMBNAIL_FILENAME";
    public static final String KEY_IMAGE_FILENAME = "IMAGE_FILENAME";
    public static final String KEY_IMAGE_ID = "IMAGE_ID";

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private FrameLayout mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private ImageViewer imageViewer;
    private AuthService authService;
    private String thumbnailFileName = "";
    private String imageUrl = "";
    private String imageFileName = "";
    private String imageFilePath = "";
    private String imageId = "";
    private ImageResultReceiver resultReceiver;
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    public ImageActivity() {
        super();
        resultReceiver = new ImageResultReceiver();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authService = AuthService.getInstance(getApplicationContext());

        Intent intent = getIntent();
        this.imageUrl = intent.getStringExtra(KEY_IMAGE_URL);
        this.thumbnailFileName = intent.getStringExtra(KEY_THUMBNAIL_FILENAME);
        this.imageFileName = intent.getStringExtra(KEY_IMAGE_FILENAME);
        this.imageId = intent.getStringExtra(KEY_IMAGE_ID);

        setContentView(R.layout.activity_image);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.image_container);
        imageViewer = new ImageViewer(getApplicationContext());

        mContentView.addView(imageViewer, 0);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

    }

    @Override
    protected void onResume() {
        if (imageFilePath.isEmpty()) {

            imageViewer.loadImage(thumbnailFileName);
            Intent intent = new Intent(ImageActivity.this, CoreService.class);
            intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
            intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.LOAD_SINGLE_IMAGE);
            intent.putExtra(CoreService.KEY_REQUEST_ID, imageId);
            startService(intent);

        } else {
            imageViewer.loadImage(imageFileName);
        }
        super.onResume();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


     private class ImageResultReceiver extends ResultReceiver {
        public ImageResultReceiver() {
            super(new Handler());
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (ImageActivity.this == null)
                return;

            switch (resultCode) {
                case CoreService.NETWORK_EXCEPTION: {
                    String message = resultData.getString(CoreService.KEY_RESULT_NETWORK_EXCEPTION);
                    Intent intent = new Intent(getApplicationContext(), NoInternetActivity.class);
                    intent.putExtra(CoreService.KEY_RESULT_NETWORK_EXCEPTION, message);
                    ImageActivity.this.startActivity(intent);
                    break;
                }

                case CoreService.IMAGE_LOADED:
                    GalleryEntity ge = resultData.getParcelable(CoreService.KEY_RESULT_GALLERY_ENTITY);
                    imageFilePath = ge.getPathToImage();
                    imageViewer.loadImage(imageFilePath);
                    break;

            }
            super.onReceiveResult(resultCode, resultData);
        }
    }
}
