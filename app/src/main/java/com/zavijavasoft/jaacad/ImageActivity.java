package com.zavijavasoft.jaacad;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;


public class ImageActivity extends AppCompatActivity {

    public static final String KEY_THUMBNAIL_FILENAME = "com.zavijavasoft.jaacad.THUMBNAIL_FILENAME";
    public static final String KEY_IMAGE_FILENAME = "com.zavijavasoft.jaacad.IMAGE_FILENAME";
    public static final String KEY_IMAGE_ID = "com.zavijavasoft.jaacad.IMAGE_ID";

    private final Handler mHideHandler = new Handler();


    private FrameLayout mContentView;

    private ImageViewer imageViewer;
    private String thumbnailFileName = "";
        private String imageFileName = "";
    private String imageFilePath = "";
    private String imageId = "";
    private long fileSize;
    private ImageResultReceiver resultReceiver;
    private View upperBarView;
    private View lowerBarView;
    private View fullScreenShadow;
    private ProgressBar progressBar;
    private ImageButton buttonLeave;
    private TextView textViewFileName;
    private TextView textViewFileSize;

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

        Intent intent = getIntent();
        this.thumbnailFileName = intent.getStringExtra(KEY_THUMBNAIL_FILENAME);
        this.imageFileName = intent.getStringExtra(KEY_IMAGE_FILENAME);
        this.imageId = intent.getStringExtra(KEY_IMAGE_ID);

        setContentView(R.layout.activity_image);

        isBarsVisible = true;
        upperBarView = findViewById(R.id.fullscreen_upper_bar);
        buttonLeave = findViewById(R.id.leave_button);
        lowerBarView = findViewById(R.id.fullscreen_lower_bar);
        textViewFileName = findViewById(R.id.image_file_name);
        textViewFileName.setText(this.imageFileName);
        textViewFileSize = findViewById(R.id.image_file_size);

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

        buttonLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageActivity.this.finish();
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
                    fileSize = new File(imageFilePath).length();
                    imageViewer.loadImage(imageFilePath);
                    textViewFileSize.setText(String.format(
                            getString(R.string.image_activity_file_size_template),
                            fileSize));
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
