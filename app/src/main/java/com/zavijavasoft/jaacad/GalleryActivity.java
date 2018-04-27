package com.zavijavasoft.jaacad;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class GalleryActivity extends AppCompatActivity
        implements ShowImageCallback {

    private static final String TAG = "GalleryActivity";


    public static final String TAG_GALLERY_FRAGMENT = "gallery_fragment";
    public static final String DEFAULT_PUBLIC_URL = "https://yadi.sk/d/wcjuTZND3URnHp";
    public static final String KEY_INTERNET_APPROVED = "KEY_INTERNET_APPROVED";

    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        // получаем экземпляр FragmentTransaction
        fragmentManager = getFragmentManager();


        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        if (null == fragmentManager.findFragmentByTag(TAG_GALLERY_FRAGMENT)) {
            // добавляем фрагмент
            GalleryFragment galleryFragment = new GalleryFragment();
            fragmentTransaction.add(R.id.gallery_container, galleryFragment, TAG_GALLERY_FRAGMENT);
        }
        fragmentTransaction.commit();

    }

    @Override
    public void showImage(String thumbnailFilename, String url, String fileName, String id) {
        Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
        intent.putExtra(ImageActivity.KEY_IMAGE_URL, url);
        intent.putExtra(ImageActivity.KEY_THUMBNAIL_FILENAME, thumbnailFilename);
        intent.putExtra(ImageActivity.KEY_IMAGE_FILENAME, fileName);
        intent.putExtra(ImageActivity.KEY_IMAGE_ID, id);
        startActivity(intent);
    }
}



