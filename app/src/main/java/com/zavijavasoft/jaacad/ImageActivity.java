package com.zavijavasoft.jaacad;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.design.widget.Snackbar;
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
    private static final String KEY_IS_LOADING_STATE = "com.zavijavasoft.jaacad.IS_LOADING_STATE";

    private final Handler handler = new Handler();


    private FrameLayout mContentView;

    private ImageViewer imageViewer;
    private String thumbnailFileName = "";
    private String imageFileName = "";

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
    private TextView textImageUnavailable;
    private View imageUnavailableView;

    private boolean isBarsVisible;

    private boolean isLoading;


    public ImageActivity() {
        super();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            isLoading = savedInstanceState.getBoolean(KEY_IS_LOADING_STATE);
            resultReceiver = savedInstanceState.getParcelable(CoreService.KEY_INTENT_RECEIVER);
        } else {
            resultReceiver = new ImageResultReceiver();
        }
        resultReceiver.setActivity(this);

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
        imageUnavailableView = findViewById(R.id.image_unavailable_icon);
        textImageUnavailable = findViewById(R.id.image_unavailable_text);
        if (isLoading)
            setLoadingMode();

        mContentView = findViewById(R.id.image_container);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        imageViewer = new ImageViewer(getApplicationContext());
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
        outState.putBoolean(KEY_IS_LOADING_STATE, isLoading);
        super.onSaveInstanceState(outState);
    }

    public void setUnavailableMode() {
        fullScreenShadow.setVisibility(View.VISIBLE);
        textImageUnavailable.setVisibility(View.VISIBLE);
        imageUnavailableView.setVisibility(View.VISIBLE);
    }

    public void setLoadingMode() {
        isLoading = true;
        fullScreenShadow.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

    }

    public void cancelLoadingMode() {
        isLoading = false;
        fullScreenShadow.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }


    @Override
    protected void onResume() {

        setLoadingMode();
        Intent intent = new Intent(ImageActivity.this, CoreService.class);
        intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
        intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.LOAD_SINGLE_IMAGE);
        intent.putExtra(CoreService.KEY_REQUEST_ID, imageId);
        startService(intent);

        super.onResume();

    }

    @Override
    protected void onStop() {
        imageViewer.destroy();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        resultReceiver.setActivity(null);
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        handler.post(new Runnable() {
            @Override
            public void run() {

                imageViewer.loadImage(thumbnailFileName);
            }
        });
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


    private static class ImageResultReceiver extends ResultReceiver {

        transient private ImageActivity A = null;

        public ImageResultReceiver() {
            super(new Handler());
        }

        public void setActivity(ImageActivity activity) {
            A = activity;
        }


        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (A == null)
                return;

            switch (resultCode) {
                case CoreService.IMAGE_UNAVAILABLE:
                    A.setUnavailableMode();
                    break;
                case CoreService.IMAGE_LOADED:
                    try {
                        A.cancelLoadingMode();
                        GalleryEntity ge = resultData.getParcelable(CoreService.KEY_RESULT_GALLERY_ENTITY);
                        String imageFilePath = ge.getPathToImage();
                        A.fileSize = new File(imageFilePath).length();
                        A.imageViewer.loadImage(imageFilePath);
                        A.textViewFileSize.setText(String.format(
                                A.getString(R.string.image_activity_file_size_template),
                                A.fileSize));
                        A.toggle();
                    } catch (OutOfMemoryError e) {
                        A.setUnavailableMode();
                        Snackbar.make(A.fullScreenShadow, e.getLocalizedMessage(), Snackbar.LENGTH_LONG);

                    }
                    break;

                case CoreService.IMAGE_LOADING_PROGRESS: {
                    long percent = resultData.getLong(CoreService.KEY_RESULT_IMAGE_DOWNLOAD_PROGRESS);
                    A.progressBar.setProgress((int) percent);
                }
            }
            super.onReceiveResult(resultCode, resultData);
        }
    }
}
