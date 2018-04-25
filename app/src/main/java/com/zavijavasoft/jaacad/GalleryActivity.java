package com.zavijavasoft.jaacad;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


public class GalleryActivity extends AppCompatActivity
    implements ShowImageCallback
{

    private static final String TAG = "GalleryActivity";


    public static final String TAG_GALLERY_FRAGMENT = "gallery_fragment";
    public static final String KEY_INTERNET_APPROVED = "KEY_INTERNET_APPROVED";

    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        // получаем экземпляр FragmentTransaction
        fragmentManager = getFragmentManager();


        if (savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = fragmentManager
                    .beginTransaction();

            // добавляем фрагмент
            GalleryFragment galleryFragment = new GalleryFragment();
            fragmentTransaction.add(R.id.gallery_container, galleryFragment, TAG_GALLERY_FRAGMENT);
            fragmentTransaction.commit();
        }

        Intent intent = getIntent();
        if (!intent.getBooleanExtra(KEY_INTERNET_APPROVED, false)) {
            AsyncCheckInternetConnection async = new AsyncCheckInternetConnection();
            async.execute(this);
        }


    }

    @Override
    public void showImage(String thumbnailFilename, String url){
        Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
        intent.putExtra(ImageActivity.KEY_IMAGE_URL, url);
        intent.putExtra(ImageActivity.KEY_THUMBNAIL_FILENAME, thumbnailFilename);
        startActivity(intent);
    }
}



