package com.zavijavasoft.jaacad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

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
   /* private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    */
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
    private View upperBarView;
    private View lowerBarView;
    private View fullScreenShadow;
    private ProgressBar progressBar;

    private boolean isBarsVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            toggle();
        }
    };
    private boolean isLoading;


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

        isBarsVisible = true;
        upperBarView = findViewById(R.id.fullscreen_upper_bar);
        lowerBarView = findViewById(R.id.fullscreen_lower_bar);

        fullScreenShadow = findViewById(R.id.fullscreen_shadow);
        progressBar = findViewById(R.id.image_progressbar);

        mContentView = findViewById(R.id.image_container);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        imageViewer = new ImageViewer(getApplicationContext());
        imageViewer.loadImage(thumbnailFileName);

        mContentView.addView(imageViewer, 0);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isLoading)
                    toggle();
            }
        });

    }

    private void setLoadingMode() {
        isLoading = true;
        fullScreenShadow.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

    }

    private void cancelLoadingMode() {
        isLoading = false;
        fullScreenShadow.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        if (imageFilePath.isEmpty()) {
            imageViewer.loadImage(thumbnailFileName);
            setLoadingMode();
            Intent intent = new Intent(ImageActivity.this, CoreService.class);
            intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
            intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.LOAD_SINGLE_IMAGE);
            intent.putExtra(CoreService.KEY_REQUEST_ID, imageId);
            startService(intent);

        } else {
            imageViewer.loadImage(imageFilePath);
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        imageViewer.destroy();
        super.onStop();
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
        if (isBarsVisible) {
            upperBarView.setVisibility(View.GONE);
            lowerBarView.setVisibility(View.GONE);
        } else {
            upperBarView.setVisibility(View.VISIBLE);
            lowerBarView.setVisibility(View.VISIBLE);
        }
        isBarsVisible = !isBarsVisible;
    }

    /*
    private void hide() {

        upperBarView.setVisibility(View.GONE);
        lowerBarView.setVisibility(View.GONE);
        isBarsVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }
*/
    /*
    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        /*mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        isBarsVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }
*/

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
                    cancelLoadingMode();
                    GalleryEntity ge = resultData.getParcelable(CoreService.KEY_RESULT_GALLERY_ENTITY);
                    imageFilePath = ge.getPathToImage();
                    imageViewer.loadImage(imageFilePath);
                    break;

                case CoreService.IMAGE_LOADING_PROGRESS: {
                    long percent = resultData.getLong(CoreService.KEY_RESULT_IMAGE_DOWNLOAD_PROGRESS);
                    progressBar.setProgress((int) percent);
                }
            }
            super.onReceiveResult(resultCode, resultData);
        }
    }
}
